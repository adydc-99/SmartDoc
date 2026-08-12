package com.smartdoc.ai;
import java.util.Optional;
public interface SecretStore { boolean isAvailable(); Optional<String> load(); void save(String secret); void clear(); }
