package com.flippingtool;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class FlippingApiClient
{
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final FlippingToolConfig config;
	
	// Cache to avoid spamming the API
	private final Map<Integer, CachedAnalysis> analysisCache = new ConcurrentHashMap<>();
	private static final long CACHE_DURATION_MS = 60_000; // 1 minute cache

	@Inject
	public FlippingApiClient(FlippingToolConfig config)
	{
		this.config = config;
		this.gson = new Gson();
		this.httpClient = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.readTimeout(10, TimeUnit.SECONDS)
			.build();
	}

	/**
	 * Fetch item analysis from the API asynchronously
	 */
	public CompletableFuture<FlipAnalysis> getItemAnalysisAsync(int itemId)
	{
		return CompletableFuture.supplyAsync(() -> getItemAnalysis(itemId));
	}

	/**
	 * Fetch item analysis from the API (synchronous)
	 */
	public FlipAnalysis getItemAnalysis(int itemId)
	{
		// Check cache first
		CachedAnalysis cached = analysisCache.get(itemId);
		if (cached != null && !cached.isExpired())
		{
			return cached.getAnalysis();
		}

		String apiUrl = config.apiUrl();
		if (apiUrl == null || apiUrl.isEmpty())
		{
			log.warn("API URL not configured");
			return null;
		}

		String url = String.format("%s/analysis/%d?timeframe=1h", apiUrl, itemId);
		Request request = new Request.Builder()
			.url(url)
			.get()
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				log.debug("API returned error for item {}: {}", itemId, response.code());
				return null;
			}

			String jsonData = response.body().string();
			FlipAnalysis analysis = gson.fromJson(jsonData, FlipAnalysis.class);

			// Cache the result
			analysisCache.put(itemId, new CachedAnalysis(analysis));

			return analysis;
		}
		catch (IOException e)
		{
			log.debug("Failed to fetch analysis for item {}: {}", itemId, e.getMessage());
			return null;
		}
	}

	/**
	 * Clear the analysis cache
	 */
	public void clearCache()
	{
		analysisCache.clear();
	}

	/**
	 * Remove a specific item from the cache
	 */
	public void invalidateCache(int itemId)
	{
		analysisCache.remove(itemId);
	}

	/**
	 * Inner class to store cached analysis with timestamp
	 */
	private static class CachedAnalysis
	{
		private final FlipAnalysis analysis;
		private final long timestamp;

		public CachedAnalysis(FlipAnalysis analysis)
		{
			this.analysis = analysis;
			this.timestamp = System.currentTimeMillis();
		}

		public FlipAnalysis getAnalysis()
		{
			return analysis;
		}

		public boolean isExpired()
		{
			return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
		}
	}
}

