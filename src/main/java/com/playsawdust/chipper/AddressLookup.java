/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xbill.DNS.Address;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.playsawdust.chipper.exception.FormatException;

public class AddressLookup {
	private static final Pattern IPV6_PATTERN = Pattern.compile("^\\[([0-9a-fA-F:]+(?:%[0-9]+)?)\\](:[0-9]+)?$");
	private static final Pattern IPV4_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+){3})(:[0-9]+)?$");
	private static final Pattern DOMAIN_PATTERN = Pattern.compile("^([^:/@\\\\]*)(:[0-9]+)?$");

	private static final Pattern LEGACY_IPV4_PATTERN = Pattern.compile("^((?:0[0-7]+|0x[0-9a-fA-F]+|[1-9][0-9]*|0)(?:\\.(?:0[0-7]+|0x[0-9a-fA-F]+|[1-9][0-9]*|0)){0,3})(:[0-9]+|(?:\\.[0-9]+){2})?$");

	private static final ImmutableList<Pattern> PATTERNS = ImmutableList.of(IPV6_PATTERN, IPV4_PATTERN, DOMAIN_PATTERN);

	/**
	 * Check if a string represents a syntatically valid address. No DNS lookups are performed and
	 * no guarantees are made as to the address's resolvability.
	 * @return {@code true} if the given address <i>appears</i> valid and will parse successfully
	 */
	public static boolean isValid(String str) {
		for (Pattern p : PATTERNS) {
			Matcher m = p.matcher(str);
			if (m.matches()) {
				Integer port = m.group(2) == null ? null : Ints.tryParse(m.group(2));
				if (port != null && (port < 1 || port > 65535)) return false;
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses and looks up an address. Will find SRV records named after {@code Distribution.ID},
	 * perform the usual DNS lookups, and parses IPv4 or IPv6 addresses with or without port numbers,
	 * defaulting to {@code Distribution.DEFAULT_PORT}.
	 * <p>
	 * Also parses legacy classful IPv4 addresses (e.g. 127.1 == 127.0.0.1), legacy hex IPv4
	 * addresses (e.g. 0x7F000001 == 127.0.0.1), legacy octal IPv4 addresses (e.g. 017700000001 ==
	 * 127.0.0.1), legacy octal or hex IPv4 octets, and NFS port numbers (e.g. 127.0.0.1.52.135 ==
	 * 127.0.0.1:13447).
	 * @throws UnknownHostException if the address cannot be resolved
	 * @throws FormatException if the address could not be parsed
	 * @see <a href="https://unascribed.com/junk/ipv4-arcana">IPv4 Arcana</a>
	 */
	public static InetSocketAddress resolve(String str) throws UnknownHostException {
		String address = null;
		Integer port = null;
		for (Pattern p : PATTERNS) {
			Matcher m = p.matcher(str);
			if (m.matches()) {
				address = m.group(1);
				port = m.group(2) == null ? null : Integer.parseInt(m.group(2).substring(1));
				break;
			}
		}
		Matcher leg = LEGACY_IPV4_PATTERN.matcher(str);
		if (leg.matches()) {
			// Java on Linux parses legacy addresses, but I'm wary of their portability, so
			// transform them to modern syntax before processing them.
			// (...why am i doing this?)
			String ohNo = leg.group(1);
			List<String> octets = Lists.newArrayList(Splitter.on('.').splitToList(ohNo));
			if (octets.size() == 3) {
				octets.add(2, "0");
			} else if (octets.size() == 2) {
				octets.add(1, "0");
				octets.add(1, "0");
			} else if (octets.size() != 1 && octets.size() != 4) {
				throw new FormatException("For input string \""+str+"\": More than 4 octets, but not NFS port number?");
			}
			ListIterator<String> iter = octets.listIterator();
			while (iter.hasNext()) {
				// convert octal or hex parts to decimal
				iter.set(Integer.toString(Integer.decode(iter.next())));
			}
			if (octets.size() == 1) {
				address = InetAddresses.fromInteger(Integer.parseInt(octets.get(0))).getHostAddress();
			} else {
				address = Joiner.on('.').join(octets);
			}
			String portStr = leg.group(2);
			if (portStr != null) {
				if (portStr.startsWith(".")) {
					List<String> parts = Lists.newArrayList(Splitter.on('.').splitToList(portStr.substring(1)));
					port = ((Integer.parseInt(parts.get(0))&0xFF) << 8) | (Integer.parseInt(parts.get(1))&0xFF);
				} else {
					port = Integer.parseInt(portStr.substring(1));
				}
			}
		}
		if (port != null && (port < 1 || port > 65535)) throw new FormatException("For input string \""+str+"\": Port number out of range");
		if (address == null) throw new FormatException("For input string \""+str+"\": Address string is not of any known format");
		if (!address.contains(":") && !Address.isDottedQuad(address) && port == null) {
			try {
				Record[] records = new Lookup("_"+Distribution.ID+"._tcp."+address, Type.SRV).run();
				if (records != null) {
					SRVRecord lowestPriority = null;
					for (Record r : records) {
						if (r instanceof SRVRecord) {
							SRVRecord srv = (SRVRecord)r;
							if (lowestPriority == null || srv.getPriority() < lowestPriority.getPriority()) {
								lowestPriority = srv;
							}
						}
					}
					if (lowestPriority != null) {
						address = lowestPriority.getTarget().toString(true);
						port = lowestPriority.getPort();
					}
				}
			} catch (TextParseException e) {
				throw (UnknownHostException)new UnknownHostException().initCause(e);
			}
		}
		if (port == null) port = Distribution.DEFAULT_PORT;
		InetAddress parsedAddr = InetAddress.getByName(address);
		return new InetSocketAddress(parsedAddr, port);
	}

}
