/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.mv;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonWritableChannelException;
import org.h2.store.fs.FileBase;
import org.h2.store.fs.FilePathWrapper;

/**
 * Adds an arbitrary header to an MVStore file, to allow easily differentiating between
 * raw MVStore files and saves from Chipper-based games.
 */
// copied from FilePathNio in MVStore, as FileNio is package-private
public abstract class FilePathNioWithHeader<T> extends FilePathWrapper {

	@Override
	public FileNioWithHeader<T> open(String mode) throws IOException {
		return new FileNioWithHeader<>(this, name.substring(getScheme().length() + 1), mode);
	}

	public void create(T header) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(name.substring(getScheme().length()+1), "rw")) {
			writeHeader(raf, header);
		}
	}

	protected abstract T readHeader(RandomAccessFile raf) throws IOException;
	protected abstract void writeHeader(RandomAccessFile raf, T header) throws IOException;

	protected abstract int getHeaderLength();

	public static class FileNioWithHeader<T> extends FileBase {
		private final FilePathNioWithHeader<T> owner;

		private final String name;
		private final RandomAccessFile raf;
		private final FileChannel channel;

		private T header;

		private final int ofs;

		protected FileNioWithHeader(FilePathNioWithHeader<T> owner, String fileName, String mode) throws IOException {
			this.owner = owner;
			this.name = fileName;
			this.raf = new RandomAccessFile(fileName, mode);
			this.channel = raf.getChannel();
			this.ofs = owner.getHeaderLength();
			try {
				setHeaderDirect(owner.readHeader(raf));
				channel.position(ofs);
			} catch (IOException e) {
				this.channel.close();
				throw e;
			}
		}

		public T getHeader() {
			return header;
		}

		private void setHeaderDirect(T header) {
			this.header = header;
		}

		public void setHeader(T header) throws IOException {
			owner.writeHeader(raf, header);
			setHeaderDirect(header);
		}

		@Override
		public void implCloseChannel() throws IOException {
			channel.close();
		}

		@Override
		public long position() throws IOException {
			return channel.position()-ofs;
		}

		@Override
		public long size() throws IOException {
			return channel.size()-ofs;
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			return channel.read(dst);
		}

		@Override
		public FileChannel position(long pos) throws IOException {
			channel.position(pos+ofs);
			return this;
		}

		@Override
		public int read(ByteBuffer dst, long position) throws IOException {
			return channel.read(dst, position+ofs);
		}

		@Override
		public int write(ByteBuffer src, long position) throws IOException {
			return channel.write(src, position+ofs);
		}

		@Override
		public FileChannel truncate(long newLength) throws IOException {
			newLength += ofs;
			long size = channel.size();
			if (newLength < size) {
				long pos = channel.position();
				channel.truncate(newLength);
				long newPos = channel.position();
				if (pos < newLength) {
					// position should stay
					// in theory, this should not be needed
					if (newPos != pos) {
						channel.position(pos);
					}
				} else if (newPos > newLength) {
					// looks like a bug in this FileChannel implementation, as
					// the documentation says the position needs to be changed
					channel.position(newLength);
				}
			}
			return this;
		}

		@Override
		public void force(boolean metaData) throws IOException {
			channel.force(metaData);
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			try {
				return channel.write(src);
			} catch (NonWritableChannelException e) {
				throw new IOException("read only");
			}
		}

		@Override
		public synchronized FileLock tryLock(long position, long size,
				boolean shared) throws IOException {
			if (!(position == 0 && size == Long.MAX_VALUE)) {
				position += ofs;
			}
			return channel.tryLock(position, size, shared);
		}

		@Override
		public String toString() {
			return owner.getScheme() + ":" + name;
		}

	}


}
