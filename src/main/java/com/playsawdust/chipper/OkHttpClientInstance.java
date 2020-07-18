/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import java.util.concurrent.TimeUnit;

import com.playsawdust.chipper.toolbox.lipstick.BraceFormatter;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;
import okhttp3.internal.Version;

/**
 * Holder for a shared OkHttpClient that is configured with useful general-purpose settings for
 * communicating with web services.
 * <p>
 * Supports Brotli response decompression, follows normal redirects, does not follow redirects
 * between HTTP and HTTPS, uses system DNS, 5 second connect/read/write timeouts, does not cache,
 * does not store cookies, sets a relevant User-Agent.
 */
public class OkHttpClientInstance {

	private static final OkHttpClient INST = new OkHttpClient.Builder()
			.addInterceptor(BrotliInterceptor.INSTANCE)
			.addInterceptor(chain -> {
				return chain.proceed(chain.request().newBuilder()
						.header("User-Agent", BraceFormatter.format("{}/{} (Chipper/{}; {})",
								Distribution.NAME, Distribution.VERSION,
								Distribution.CHIPPER_VERSION, Version.userAgent.replace("okhttp", "OkHttp"))
							)
						.build());
			})
			.cache(null)
			.dns(Dns.SYSTEM)
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.writeTimeout(5, TimeUnit.SECONDS)
			.followRedirects(true)
			.followSslRedirects(false)
			.build();

	public static OkHttpClient get() {
		return INST;
	}

}
