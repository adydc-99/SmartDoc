package com.smartdoc.ai;
public class AiQuotaExceededException extends RuntimeException { public AiQuotaExceededException(){super("Daily AI request limit reached");} }
