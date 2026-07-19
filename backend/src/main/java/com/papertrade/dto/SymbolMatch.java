package com.papertrade.dto;

/**
 * A single stock-search result: ticker symbol + company description.
 */
public record SymbolMatch(String symbol, String description) {}
