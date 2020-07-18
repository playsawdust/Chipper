/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.server.dedicated.jediterm;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.google.common.base.Charsets;
import com.jediterm.terminal.Questioner;
import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.playsawdust.chipper.Distribution;

import com.playsawdust.chipper.toolbox.concurrent.SharedThreadPool;
import com.playsawdust.chipper.toolbox.io.TeeOutputStream;

public class JediTermController {

	private JFrame frame;
	private JediTermWidget jt;
	private PipedInputStream stdinPipeIn;
	private PipedOutputStream stdoutPipeOut;

	private int stopAttempts = 0;
	private long lastStopAttempt = 0;

	private final PrintStream originalErr = System.err;

	public void start() {
		if (!GraphicsEnvironment.isHeadless()) {
			try {
				// Java on Linux has the Metal LaF as the system LaF, but a GTK+ based LaF is available
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (Exception e) {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e2) {
					try {
						UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					} catch (Exception e3) {
						// give up
					}
				}
			}
			frame = new JFrame(Distribution.NAME+" Server");
			Font f;
			if (Distribution.SERVER_TERMINAL_FONT_TTF != null) {
				try (InputStream in = ClassLoader.getSystemResourceAsStream(Distribution.SERVER_TERMINAL_FONT_TTF)) {
					f = Font.createFont(Font.TRUETYPE_FONT, in);
				} catch (Exception e1) {
					throw new RuntimeException("Invalid dist.jkson; couldn't load server terminal font", e1);
				}
			} else {
				f = null;
			}
			jt = new JediTermWidget(new DefaultSettingsProvider() {
				@Override
				public TextStyle getDefaultStyle() {
					return new TextStyle(TerminalColor.awt(new Color(0xCFD8DC)), TerminalColor.awt(new Color(0x263238)));
				}
				@Override
				public boolean useAntialiasing() {
					return true;
				}
				@Override
				public Font getTerminalFont() {
					return f == null ? super.getTerminalFont() : f.deriveFont(getTerminalFontSize());
				}
				@Override
				public float getTerminalFontSize() {
					return 16;
				}
				@Override
				public ColorPalette getTerminalColorPalette() {
					// based on my personal Material-y Konsole theme -Una
					return new ColorPalette() {
						@Override
						public Color[] getIndexColors() {
							return new Color[] {
									// normal
									new Color(0x455A64),
									new Color(0xFF3D00),
									new Color(0x00E676),
									new Color(0xFF9100),
									new Color(0x3D5AFE),
									new Color(0x7C4DFF),
									new Color(0x00B0FF),
									new Color(0xDCDCDC),

									// intense
									new Color(0x607D8B),
									new Color(0xFF1744),
									new Color(0x1DE9B6),
									new Color(0xFFC400),
									new Color(0x2979FF),
									new Color(0xD500F9),
									new Color(0x00E5FF),
									new Color(0xFFFFFF)
							};
						}

					};
				}
			});
			frame.setContentPane(jt.getComponent());
			try {
				PipedOutputStream stdinPipeOut = new PipedOutputStream();
				stdinPipeIn = new PipedInputStream(stdinPipeOut);
				OutputStreamWriter stdinPipeWriter = new OutputStreamWriter(stdinPipeOut, Charsets.UTF_8);

				stdoutPipeOut = new PipedOutputStream();
				PipedInputStream stdoutPipeIn = new PipedInputStream(stdoutPipeOut);
				InputStreamReader stdoutPipeReader = new InputStreamReader(stdoutPipeIn, Charsets.UTF_8);
				TtyConnector tty = new TtyConnector() {

					@Override
					public void write(String string) throws IOException {
						stdinPipeWriter.write(string);
					}

					@Override
					public void write(byte[] bytes) throws IOException {
						stdinPipeOut.write(bytes);
					}

					@Override
					public int waitFor() throws InterruptedException {
						throw new UnsupportedOperationException();
					}

					@Override
					public void resize(Dimension termSize, Dimension pixelSize) {
					}

					@Override
					public int read(char[] buf, int offset, int length) throws IOException {
						int count = stdoutPipeReader.read(buf, offset, length);
						String s = new String(buf, offset, count).replace("\n", "\r\n");
						s.getChars(0, Math.min(s.length(), length), buf, offset);
						return Math.min(s.length(), length);
					}

					@Override
					public boolean isConnected() {
						return true;
					}

					@Override
					public boolean init(Questioner q) {
						return true;
					}

					@Override
					public String getName() {
						return Distribution.NAME;
					}

					@Override
					public void close() {
						throw new UnsupportedOperationException();
					}
				};
				jt.createTerminalSession(tty);
				jt.start();
				System.setIn(stdinPipeIn);
				System.setOut(new PrintStream(new TeeOutputStream(System.out, stdoutPipeOut)));
				System.setErr(new PrintStream(new TeeOutputStream(System.err, stdoutPipeOut)));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			Image icon;
			if (Distribution.SERVER_TERMINAL_ICON != null) {
				try {
					icon = ImageIO.read(ClassLoader.getSystemResource(Distribution.SERVER_TERMINAL_ICON));
					frame.setIconImage(icon);
				} catch (Exception e1) {
					throw new RuntimeException("Invalid dist.jkson; couldn't load server terminal icon", e1);
				}
			}
			frame.setLocationByPlatform(true);
			frame.setSize(1280, 720);
			frame.setVisible(true);
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			final JediTermWidget jtf = jt;
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					try {
						jtf.getCurrentSession().getTtyConnector().write("stop\n");
					} catch (IOException e1) {}
					if (System.currentTimeMillis()-lastStopAttempt > 5000) {
						stopAttempts = 0;
					}
					stopAttempts++;
					if (lastStopAttempt == 0) {
						SharedThreadPool.schedule(() -> {
							SwingUtilities.invokeLater(() -> {
								int response = JOptionPane.showConfirmDialog(frame,
										"<html>"
										+ "The server may be stuck trying to stop.<br/>"
										+ "Would you like to force stop it?<br/>"
										+ "<b>Any unsaved data will be lost, and map corruption may occur!</b>"
										+ "</html>",
										"Force stop?",
										JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
								if (response == JOptionPane.YES_OPTION) {
									originalErr.println("Force exiting due to user request!");
									Runtime.getRuntime().halt(111);
								}
							});
						}, 5, TimeUnit.SECONDS);
					}
					lastStopAttempt = System.currentTimeMillis();
					if (stopAttempts > 4) {
						int response = JOptionPane.showConfirmDialog(frame,
								"<html>"
								+ "You've pressed the close button a lot.<br/>"
								+ "Would you like to force stop the server?<br/>"
								+ "<b>Any unsaved data will be lost, and map corruption may occur!</b>"
								+ "</html>",
								"Force stop?",
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
						if (response == JOptionPane.YES_OPTION) {
							originalErr.println("Force exiting due to user request!");
							Runtime.getRuntime().halt(111);
						}
					}
				}
			});
		}
	}

	public void stop() {
		if (!GraphicsEnvironment.isHeadless()) {
			try {
				stdoutPipeOut.write("\n[ server exited ]".getBytes(Charsets.UTF_8));
				stdoutPipeOut.close();
				stdinPipeIn.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		}
	}

}
