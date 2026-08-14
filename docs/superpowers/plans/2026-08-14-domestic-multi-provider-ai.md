# Domestic Multi-Provider AI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace SmartDoc's single global DeepSeek setting with owner-scoped, secure, OpenAI-compatible provider configuration, domestic provider presets, text/vision routing, and cached vision-to-text collaboration.

**Architecture:** Store provider metadata per user, keep API keys encrypted with an environment master key or volatile in memory, and route requests by declared capability rather than vendor name. A protocol-level `ProviderAdapter` handles OpenAI Chat Completions text and `image_url` messages; the existing document AI actions use the default text provider while a separate bounded vision endpoint can answer directly or pass a cached structured observation to the text provider.

**Tech Stack:** Java 11, Spring Boot 2.7.18, MyBatis Plus 3.5.5, H2/MySQL 8, PDFBox 2.0.31, Vue 3.5, TypeScript 5.7, Pinia 4, Element Plus 2.9, Vitest 4, Maven, pnpm.

## Global Constraints

- Supported execution paths are exactly offline `DEMO` and persisted providers using `OPENAI_CHAT_COMPLETIONS`; Demo is not stored as a network provider.
- Presets are editable defaults, not compatibility guarantees; custom compatible endpoints remain available.
- API keys never appear in response DTOs, URLs, logs, Git, browser persistence, or plaintext database columns.
- With no master key configured, provider keys are process-memory only and disappear on restart; never fall back to plaintext persistence.
- HTTPS is required for remote endpoints; HTTP is allowed only for loopback unless an administrator explicitly enables private-network endpoints.
- AI calls occur only after an explicit user action; document upload and parsing never trigger paid AI automatically.
- Text and vision defaults are selected independently and every provider query is owner-scoped at the SQL boundary.
- Vision input is PNG/JPEG/WebP, at most 8 MiB after decoding; a PDF request renders only one owned page at 144 DPI.
- CI uses mock HTTP services only and never calls a paid provider.
- MySQL schema scripts are generated but never executed automatically against an existing database.

---

## File Map

New backend units:

- `backend/src/main/java/com/smartdoc/ai/provider/AiProviderConfig.java` — persisted owner-scoped provider metadata and encrypted-key envelope.
- `backend/src/main/java/com/smartdoc/ai/provider/AiProviderProtocol.java` — supported wire protocols.
- `backend/src/main/java/com/smartdoc/ai/provider/AiProviderPreset.java` — safe domestic preset metadata without secrets.
- `backend/src/main/java/com/smartdoc/ai/provider/AiRoutingConfig.java` — default text and vision provider IDs.
- `backend/src/main/java/com/smartdoc/ai/provider/AiProviderMapper.java` and `AiRoutingMapper.java` — fixed owner-scoped SQL.
- `backend/src/main/java/com/smartdoc/ai/provider/AiProviderService.java` — validation, CRUD, default routing, and key lifecycle.
- `backend/src/main/java/com/smartdoc/ai/provider/ProviderSecretVault.java` — encrypted database/volatile-memory secret selection.
- `backend/src/main/java/com/smartdoc/ai/provider/AesGcmSecretCipher.java` — authenticated encryption only.
- `backend/src/main/java/com/smartdoc/ai/provider/ProviderAdapter.java` — protocol-level text, vision, and probe contract.
- `backend/src/main/java/com/smartdoc/ai/provider/OpenAiChatCompletionsAdapter.java` — generic compatible request/response implementation.
- `backend/src/main/java/com/smartdoc/ai/provider/ModelRouter.java` — resolves an owned enabled provider by capability.
- `backend/src/main/java/com/smartdoc/ai/provider/ProviderController.java` — owner-scoped REST API.
- `backend/src/main/java/com/smartdoc/ai/vision/VisionAction*.java` — bounded request/response/controller/service types.
- `backend/src/main/java/com/smartdoc/ai/vision/VisionCacheRecord.java` and `VisionCacheMapper.java` — versioned observation cache.
- `backend/src/main/java/com/smartdoc/ai/vision/PdfPageImageRenderer.java` — one-page PDF rendering.

Existing backend units to modify:

- `backend/src/main/java/com/smartdoc/ai/AiClient.java`, `RoutingAiClient.java`, `AiActionService.java` — pass user identity into routing and cache identity.
- `backend/src/main/java/com/smartdoc/config/AppConfig.java` — construct the secret vault, adapter, router, and safe HTTP client.
- `backend/src/main/resources/schema.sql`, `scripts/init_mysql.sql` — fresh database schema.
- `scripts/alter_ai_provider_config.sql` — one-time existing-MySQL migration.
- `backend/src/main/resources/application.yml`, `.env.example`, `docker-compose.yml` — master-key and network policy configuration.

Frontend units:

- `frontend/src/api/providers.ts` — typed provider/routing/vision contracts.
- `frontend/src/stores/aiProviders.ts` — provider metadata only; API keys remain action arguments.
- `frontend/src/components/settings/ProviderForm.vue` and `ProviderList.vue` — CRUD, test, masking, and defaults.
- `frontend/src/views/SettingsView.vue` — hosts the new configuration flow.
- `frontend/src/views/ReaderView.vue` — visual action controls and model disclosure.

Release units:

- `.github/workflows/ci.yml`, `.github/workflows/secret-scan.yml` — build/test and tracked-file secret scanning.
- `README.md`, `.env.example` — honest compatibility, setup, and security documentation.

---

### Task 1: Owner-Scoped Provider Metadata and Routing Schema

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiProviderProtocol.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiProviderConfig.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiRoutingConfig.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiProviderMapper.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiRoutingMapper.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/AiProviderOwnershipIntegrationTest.java`
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `scripts/init_mysql.sql`
- Create: `scripts/alter_ai_provider_config.sql`

**Interfaces:**
- Produces: `AiProviderMapper.selectOwned(long id, long userId)`, `selectOwnedEnabled(...)`, `listOwned(long userId)`, and `AiRoutingMapper.selectOwned(long userId)`.
- Produces: schema consumed by all later tasks; no controller or secret writes yet.

- [ ] **Step 1: Write the failing ownership integration test**

```java
@SpringBootTest
class AiProviderOwnershipIntegrationTest {
  @Autowired AiProviderMapper providers;

  @Test void providerLookupAlwaysRequiresItsOwner() {
    AiProviderConfig row = AiProviderConfig.textProvider(41L, "DeepSeek", "CUSTOM",
        "https://api.deepseek.com/v1", "deepseek-chat");
    assertEquals(1, providers.insert(row));
    assertNotNull(providers.selectOwned(row.getId(), 41L));
    assertNull(providers.selectOwned(row.getId(), 42L));
    assertTrue(providers.listOwned(42L).isEmpty());
  }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `cd backend && mvn -q -Dtest=AiProviderOwnershipIntegrationTest test`

Expected: compilation fails because `AiProviderConfig` and `AiProviderMapper` do not exist.

- [ ] **Step 3: Add exact schema to H2, fresh MySQL, and incremental MySQL scripts**

```sql
CREATE TABLE ai_provider_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  preset_code VARCHAR(40) NOT NULL,
  protocol VARCHAR(40) NOT NULL,
  base_url VARCHAR(500) NOT NULL,
  model VARCHAR(120) NOT NULL,
  supports_text BOOLEAN NOT NULL DEFAULT TRUE,
  supports_vision BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  encrypted_api_key CLOB,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_ai_provider_user_name UNIQUE (user_id, display_name)
);
CREATE INDEX idx_ai_provider_user_enabled ON ai_provider_config(user_id, enabled);

CREATE TABLE ai_routing_config (
  user_id BIGINT PRIMARY KEY,
  default_text_provider_id BIGINT,
  default_vision_provider_id BIGINT,
  daily_limit INT NOT NULL DEFAULT 50,
  max_output_tokens INT NOT NULL DEFAULT 1024,
  updated_at TIMESTAMP NOT NULL
);
```

Use `TEXT` instead of `CLOB` in both MySQL scripts. In this task, `alter_ai_provider_config.sql` contains the two provider/routing tables and their indexes; Task 6 appends the vision-cache table. The file must carry a header stating it is a one-time manual migration.

- [ ] **Step 4: Implement entities and fixed SQL mappers**

```java
public enum AiProviderProtocol { OPENAI_CHAT_COMPLETIONS }

public interface AiProviderMapper extends BaseMapper<AiProviderConfig> {
  @Select("SELECT * FROM ai_provider_config WHERE id=#{id} AND user_id=#{userId}")
  AiProviderConfig selectOwned(@Param("id") long id, @Param("userId") long userId);

  @Select("SELECT * FROM ai_provider_config WHERE id=#{id} AND user_id=#{userId} AND enabled=TRUE")
  AiProviderConfig selectOwnedEnabled(@Param("id") long id, @Param("userId") long userId);

  @Select("SELECT * FROM ai_provider_config WHERE user_id=#{userId} ORDER BY created_at,id")
  List<AiProviderConfig> listOwned(@Param("userId") long userId);
}
```

`AiProviderConfig` must map every column, expose conventional getters/setters for MyBatis, and provide `textProvider(...)` only for tests. `AiRoutingMapper` must use `SELECT ... WHERE user_id=#{userId}` and a MySQL-compatible update-then-insert service flow rather than vendor-specific UPSERT syntax.

- [ ] **Step 5: Add routing ownership and unique-name tests**

Add tests that prove: another user cannot resolve defaults through a join; duplicate display names fail for the same user but succeed for different users; provider order is deterministic.

- [ ] **Step 6: Run focused and full backend tests**

Run: `cd backend && mvn -q -Dtest=AiProviderOwnershipIntegrationTest test`

Expected: all focused tests pass.

Run: `cd backend && mvn test`

Expected: full backend suite passes with zero failures/errors.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/smartdoc/ai/provider backend/src/test/java/com/smartdoc/ai/provider backend/src/main/resources/schema.sql scripts/init_mysql.sql scripts/alter_ai_provider_config.sql
git commit -m "feat: add owner scoped AI providers"
```

---

### Task 2: Secure Multi-Provider Key Vault

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/provider/SecretCipher.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AesGcmSecretCipher.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderSecretVault.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderKeyMissingException.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/AesGcmSecretCipherTest.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/ProviderSecretVaultTest.java`
- Modify: `backend/src/main/java/com/smartdoc/config/AppConfig.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/provider/AiProviderMapper.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `.env.example`
- Modify: `docker-compose.yml`

**Interfaces:**
- Produces: `ProviderSecretVault.save(long userId, long providerId, String apiKey, AiProviderConfig row)`, `load(...)`, and `clear(...)`.
- Consumes: `AiProviderConfig.encryptedApiKey` from Task 1.

- [ ] **Step 1: Write failing authenticated-encryption tests**

```java
@Test void encryptsWithRandomNonceAndRejectsTampering() {
  SecretCipher cipher = AesGcmSecretCipher.fromBase64(KEY_32_BYTES_BASE64);
  String a = cipher.encrypt("sk-secret", "41:7");
  String b = cipher.encrypt("sk-secret", "41:7");
  assertNotEquals(a, b);
  assertEquals("sk-secret", cipher.decrypt(a, "41:7"));
  assertThrows(SecretPersistenceException.class,
      () -> cipher.decrypt(a.substring(0, a.length() - 2) + "AA", "41:7"));
}
```

Also assert that decrypting with associated data `42:7` fails, so ciphertext cannot be copied between users.

- [ ] **Step 2: Verify RED**

Run: `cd backend && mvn -q -Dtest=AesGcmSecretCipherTest,ProviderSecretVaultTest test`

Expected: compilation fails because the cipher/vault types do not exist.

- [ ] **Step 3: Implement AES-256-GCM without adding a dependency**

```java
public final class AesGcmSecretCipher implements SecretCipher {
  private static final int NONCE_BYTES=12, TAG_BITS=128;
  private final SecretKey key; private final SecureRandom random=new SecureRandom();
  public String encrypt(String plain,String aad){
    try { byte[] nonce=new byte[NONCE_BYTES];random.nextBytes(nonce);
      Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(TAG_BITS,nonce));
      c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
      byte[] body=c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      ByteBuffer out=ByteBuffer.allocate(nonce.length+body.length).put(nonce).put(body);
      return Base64.getEncoder().encodeToString(out.array());
    } catch (GeneralSecurityException e) { throw new SecretPersistenceException("AI key encryption failed",e); }
  }
}
```

`fromBase64` must require exactly 32 decoded bytes. `decrypt` must split the first 12 bytes as nonce and use the same AAD. Exception messages must never include ciphertext or plaintext.

- [ ] **Step 4: Implement encrypted-or-volatile vault behavior**

```java
public Optional<String> load(long userId,long providerId,AiProviderConfig row) {
  String aad=userId+":"+providerId;
  if(cipher.isPresent() && row.getEncryptedApiKey()!=null)
    return Optional.of(cipher.get().decrypt(row.getEncryptedApiKey(),aad));
  return Optional.ofNullable(volatileKeys.get(userId+":"+providerId));
}
```

When a master key exists, `save` updates only the owned row's encrypted column in the same service transaction. Without it, `save` leaves the column null and stores the key in a `ConcurrentHashMap`. `clear` removes both representations and checks the mapper update count. Expose only `persistenceAvailable` and `keyConfigured`, never a decrypt endpoint.

Add these fixed owner-scoped mapper operations:

```java
@Update("UPDATE ai_provider_config SET encrypted_api_key=#{ciphertext},updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId}")
int updateEncryptedKeyOwned(@Param("id")long id,@Param("userId")long userId,@Param("ciphertext")String ciphertext,@Param("updatedAt")LocalDateTime updatedAt);
@Update("UPDATE ai_provider_config SET encrypted_api_key=NULL,updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId}")
int clearEncryptedKeyOwned(@Param("id")long id,@Param("userId")long userId,@Param("updatedAt")LocalDateTime updatedAt);
```

- [ ] **Step 5: Wire the master key and test absence/presence behavior**

Use property `smartdoc.ai.master-key-base64: ${SMARTDOC_AI_MASTER_KEY_BASE64:}`. In `.env.example`, include only `SMARTDOC_AI_MASTER_KEY_BASE64=replace-with-base64-of-32-random-bytes`. Docker must pass it through without a default real secret.

Add tests proving: blank master key uses memory and stores no ciphertext; configured key stores non-plaintext ciphertext; clear makes `load` empty; mapper zero-row updates fail closed.

- [ ] **Step 6: Run tests and commit**

Run: `cd backend && mvn -q -Dtest=AesGcmSecretCipherTest,ProviderSecretVaultTest test && mvn test`

Expected: focused and full suites pass.

```powershell
git add backend/src/main/java/com/smartdoc/ai/provider backend/src/test/java/com/smartdoc/ai/provider backend/src/main/java/com/smartdoc/config/AppConfig.java backend/src/main/resources/application.yml .env.example docker-compose.yml
git commit -m "feat: protect provider API keys"
```

---

### Task 3: Generic Text Adapter and Capability Router

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderCapability.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/TextCompletionRequest.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderResponse.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderAdapter.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/OpenAiChatCompletionsAdapter.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderAdapterRegistry.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderSession.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ModelRouter.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderHttpException.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/OpenAiChatCompletionsAdapterTest.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/ModelRouterTest.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/AiClient.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/RoutingAiClient.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/AiActionService.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/DailyAiQuota.java`
- Modify: `backend/src/main/java/com/smartdoc/config/AppConfig.java`
- Delete after migration: `backend/src/main/java/com/smartdoc/ai/DeepSeekClientFactory.java`

**Interfaces:**
- Produces: `ProviderResponse ModelRouter.complete(long userId, ProviderCapability capability, String system, String prompt)`.
- Changes: `AiClient.complete(long userId, String system, String prompt)`, `mode(long userId)`, and `model(long userId)`.
- Consumes: owned provider settings and secrets from Tasks 1–2.

- [ ] **Step 1: Write adapter contract tests against `MockRestServiceServer`**

```java
server.expect(requestTo("https://example.cn/v1/chat/completions"))
  .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer key-value"))
  .andExpect(jsonPath("$.model").value("domestic-model"))
  .andExpect(jsonPath("$.messages[0].role").value("system"))
  .andExpect(jsonPath("$.messages[1].content").value("question"))
  .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"answer\"}}]}",MediaType.APPLICATION_JSON));
assertEquals("answer",adapter.complete(config,"key-value",request).getContent());
```

Add exact tests for trailing slash normalization, empty choices, invalid JSON, body larger than configured limit, 401/403, 404, 429, 5xx, connect timeout, and read timeout. Assert error objects contain a stable code but no key or raw provider body.

- [ ] **Step 2: Verify RED**

Run: `cd backend && mvn -q -Dtest=OpenAiChatCompletionsAdapterTest,ModelRouterTest test`

Expected: compilation fails for missing adapter/router types.

- [ ] **Step 3: Define protocol-level interfaces**

```java
public interface ProviderAdapter {
  AiProviderProtocol protocol();
  ProviderResponse complete(AiProviderConfig config,String apiKey,TextCompletionRequest request);
  ProviderResponse vision(AiProviderConfig config,String apiKey,VisionCompletionRequest request);
  ProviderResponse probe(AiProviderConfig config,String apiKey);
}

public enum ProviderCapability { TEXT, VISION }

public final class ProviderSession {
  private final AiProviderConfig provider;
  private final String apiKey;
  private final ProviderAdapter adapter;
  public ProviderSession(AiProviderConfig provider,String apiKey,ProviderAdapter adapter){
    this.provider=provider;this.apiKey=apiKey;this.adapter=adapter;
  }
  public AiProviderConfig getProvider(){return provider;}
  public String getApiKey(){return apiKey;}
  public ProviderAdapter getAdapter(){return adapter;}
}
```

`ProviderAdapterRegistry.require(protocol)` must fail with code `UNSUPPORTED_PROTOCOL`; it must not inspect `presetCode` or vendor names.

- [ ] **Step 4: Implement bounded OpenAI Chat Completions text requests**

Construct the JSON with `model`, `temperature=0.2`, `max_tokens`, and ordered messages. Use Jackson tree parsing and require a nonblank `choices[0].message.content`. Build the endpoint with `URI.resolve` only after Base URL validation; never concatenate user input into headers or logs.

Use `SimpleClientHttpRequestFactory` with 5-second connect and 30-second read timeouts. Add a buffering cap by rejecting declared/actual response bodies above 2 MiB before JSON processing.

- [ ] **Step 5: Implement owner- and capability-aware routing**

```java
public ProviderSession require(long userId,ProviderCapability capability) {
  AiRoutingConfig routing=routingMapper.selectOwned(userId);
  Long id=capability==TEXT?routing.getDefaultTextProviderId():routing.getDefaultVisionProviderId();
  AiProviderConfig provider=providerMapper.selectOwnedEnabled(id,userId);
  if(provider==null || !provider.supports(capability)) throw new ProviderHttpException("MODEL_NOT_CONFIGURED",400);
  String key=vault.load(userId,id,provider).orElseThrow(ProviderKeyMissingException::new);
  return new ProviderSession(provider,key,registry.require(provider.getProtocol()));
}
```

Demo is selected when the user's routing row has no text provider. It must remain offline. A configured but keyless provider must not silently fall back to Demo.

- [ ] **Step 6: Make quota and existing AI actions user-aware**

Change `DailyAiQuota` to count by `(userId, LocalDate)` and call `consume(userId,dailyLimit)`. Pass `userId` from `AiActionService` into `AiClient`; include protocol/provider/model in cache identity so switching providers cannot reuse the wrong result.

Update existing tests to prove user A's quota/model/cache do not affect user B. Preserve the existing `AiAction` whitelist and document ownership checks.

- [ ] **Step 7: Run focused, regression, and full tests**

Run: `cd backend && mvn -q -Dtest=OpenAiChatCompletionsAdapterTest,ModelRouterTest,RoutingAiClientTest,AiActionServiceTest test`

Expected: focused tests pass, including all error mappings and two-user routing.

Run: `cd backend && mvn test`

Expected: full suite passes.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/smartdoc/ai backend/src/main/java/com/smartdoc/config/AppConfig.java backend/src/test/java/com/smartdoc/ai
git commit -m "feat: route AI through compatible providers"
```

---

### Task 4: Provider CRUD, Presets, URL Policy, and Connection Test API

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiProviderPreset.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderCreateRequest.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderUpdateRequest.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderView.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/RoutingUpdateRequest.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/AiProviderService.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderController.java`
- Create: `backend/src/main/java/com/smartdoc/ai/provider/ProviderUrlPolicy.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/ProviderControllerTest.java`
- Create: `backend/src/test/java/com/smartdoc/ai/provider/ProviderUrlPolicyTest.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/AiSettingsController.java`
- Modify: `backend/src/main/java/com/smartdoc/config/AppConfig.java`

**Interfaces:**
- Produces: `/api/ai/providers`, `/api/ai/routing`, `/api/ai/provider-presets`.
- Consumes: mapper/vault/router/adapter contracts from Tasks 1–3.

- [ ] **Step 1: Write failing MockMvc ownership and secret-response tests**

```java
mockMvc.perform(post("/api/ai/providers").header("Authorization",token41)
    .contentType(APPLICATION_JSON).content("{\"displayName\":\"My Qwen\",\"presetCode\":\"QWEN\",\"protocol\":\"OPENAI_CHAT_COMPLETIONS\",\"baseUrl\":\"https://example.cn/v1\",\"model\":\"qwen-vl\",\"apiKey\":\"secret\",\"supportsText\":true,\"supportsVision\":true}"))
  .andExpect(status().isCreated())
  .andExpect(jsonPath("$.apiKey").doesNotExist())
  .andExpect(jsonPath("$.maskedKey").value("********"));
```

Add tests that user 42 receives 404 when reading/updating/testing/deleting user 41's provider; blank update key preserves the old key; explicit `/key` deletion clears it; deleting a provider referenced by routing returns 409.

- [ ] **Step 2: Verify RED**

Run: `cd backend && mvn -q -Dtest=ProviderControllerTest,ProviderUrlPolicyTest test`

Expected: 404 because endpoints do not exist.

- [ ] **Step 3: Define immutable domestic presets**

```java
public enum AiProviderPreset {
  DEEPSEEK("DeepSeek","https://api.deepseek.com/v1",true,false),
  QWEN("阿里云百炼","https://dashscope.aliyuncs.com/compatible-mode/v1",true,true),
  VOLCENGINE("火山方舟","https://ark.cn-beijing.volces.com/api/v3",true,true),
  ZHIPU("智谱","https://open.bigmodel.cn/api/paas/v4",true,false),
  HUNYUAN("腾讯混元","https://api.hunyuan.cloud.tencent.com/v1",true,true),
  QIANFAN("百度千帆","https://qianfan.baidubce.com/v3",true,false),
  SILICONFLOW("硅基流动","https://api.siliconflow.cn/v1",true,false),
  CUSTOM("自定义兼容服务","",true,false);
}
```

Presets contain no model version, price, or API key. Their capability flags are editable suggestions and the UI must state that actual support depends on the selected model.

- [ ] **Step 4: Implement strict validation and URL policy**

Validate display name 1–80 code points, model 1–120, Base URL at most 500 characters, token limit 128–4096, daily limit 1–500, and at least one capability. Reject userinfo, fragments, query strings, non-HTTPS remote URLs, cloud metadata addresses, resolved loopback/private/link-local targets, and DNS rebinding across redirects. Disable redirects in the provider HTTP client.

Allow `http://localhost`, `http://127.0.0.1`, and `http://[::1]` only when `smartdoc.ai.allow-loopback=true`. Allow other private addresses only when `smartdoc.ai.allow-private-network=true`; both default false in Docker and true/false explicitly documented for local development.

- [ ] **Step 5: Implement controller and transactional service**

Every method obtains `CurrentUser.from(request).getUserId()`. Provider creation inserts metadata first, then stores the key with compensation if secret storage fails. Updates must use owner-scoped update counts. `test` loads the owned key, performs exactly one `probe`, consumes quota, and returns `{success,providerId,model,latencyMs}` without provider response text.

Replace the legacy settings controller with `410 Gone` plus migration guidance only after the new frontend is merged in Task 5; until then keep it functional and mark it deprecated.

- [ ] **Step 6: Run focused and full tests**

Run: `cd backend && mvn -q -Dtest=ProviderControllerTest,ProviderUrlPolicyTest test && mvn test`

Expected: all tests pass; no serialized response contains `apiKey` or `encryptedApiKey`.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/smartdoc/ai backend/src/test/java/com/smartdoc/ai backend/src/main/java/com/smartdoc/config/AppConfig.java
git commit -m "feat: manage domestic AI providers"
```

---

### Task 5: Provider Management UI

**Files:**
- Create: `frontend/src/api/providers.ts`
- Create: `frontend/src/stores/aiProviders.ts`
- Create: `frontend/src/components/settings/ProviderForm.vue`
- Create: `frontend/src/components/settings/ProviderList.vue`
- Create: `frontend/src/components/settings/__tests__/ProviderForm.spec.ts`
- Create: `frontend/src/components/settings/__tests__/ProviderList.spec.ts`
- Create: `frontend/src/stores/__tests__/aiProviders.spec.ts`
- Modify: `frontend/src/views/SettingsView.vue`
- Modify: `frontend/src/api/__tests__/aiContract.spec.ts`
- Delete after migration: `frontend/src/stores/aiSettings.ts`
- Delete after migration: `frontend/src/stores/__tests__/aiSettings.spec.ts`

**Interfaces:**
- Consumes: Task 4 provider/routing endpoints.
- Produces: settings UI used before Task 6 visual actions.

- [ ] **Step 1: Write failing API/store tests proving keys are transient**

```ts
it('clears the key argument even when save fails', async () => {
  const form = reactive({apiKey:'secret-value',displayName:'Qwen',presetCode:'QWEN',protocol:'OPENAI_CHAT_COMPLETIONS' as const,baseUrl:'https://example.cn/v1',model:'qwen-vl',supportsText:true,supportsVision:true,enabled:true})
  mockCreate.mockRejectedValueOnce(new Error('network'))
  await expect(store.create(form)).rejects.toThrow()
  expect(form.apiKey).toBe('')
  expect(JSON.stringify(store.$state)).not.toContain('secret-value')
})
```

Also inspect `localStorage` and `sessionStorage` after create/update/test; neither may contain the key. Contract tests must assert requests use `/ai/providers` and never use query parameters for secrets.

- [ ] **Step 2: Verify RED**

Run: `cd frontend && pnpm test -- src/stores/__tests__/aiProviders.spec.ts src/components/settings/__tests__/ProviderForm.spec.ts`

Expected: test files fail because the new modules do not exist.

- [ ] **Step 3: Add exact typed API contracts**

```ts
export type ProviderProtocol='OPENAI_CHAT_COMPLETIONS'
export interface AiProviderView {id:number;displayName:string;presetCode:string;protocol:ProviderProtocol;baseUrl:string;model:string;supportsText:boolean;supportsVision:boolean;enabled:boolean;maskedKey:string;keyConfigured:boolean;persistenceAvailable:boolean}
export interface RoutingView {defaultTextProviderId:number|null;defaultVisionProviderId:number|null;dailyLimit:number;maxOutputTokens:number;todayUsed:number}
export interface ProviderSecretInput {apiKey:string}
```

Keep `apiKey` out of `AiProviderView` and Pinia state types. Pass it only as a local component value into `createProvider`/`updateProvider`, then clear it in `finally`.

- [ ] **Step 4: Build provider form/list behavior**

`ProviderForm.vue` must offer preset, display name, Base URL, model, text/vision checkboxes, enabled flag, password input, and warning for custom URLs. Changing a preset fills only untouched fields. It must not guess a model name.

`ProviderList.vue` must show capability badges, key configured/persistence state, test/edit/delete controls, and default text/vision selectors. Testing displays latency/model or the normalized backend error. Key deletion requires explicit confirmation.

- [ ] **Step 5: Replace the settings page and remove legacy store usage**

Keep the existing security explanation, add the compatibility disclaimer, and ensure all labels are valid UTF-8 Chinese. Preserve keyboard labels, visible focus, `aria-live` statuses, and responsive layout. The settings page must load presets, providers, and routing in parallel without storing secret input in Pinia.

- [ ] **Step 6: Run focused tests, all tests, and build**

Run: `cd frontend && pnpm test -- src/stores/__tests__/aiProviders.spec.ts src/components/settings/__tests__/ProviderForm.spec.ts src/components/settings/__tests__/ProviderList.spec.ts src/api/__tests__/aiContract.spec.ts`

Expected: focused tests pass.

Run: `cd frontend && pnpm test && pnpm build`

Expected: all tests pass and production build exits 0; pre-existing bundle-size warnings may remain documented but no new warning is accepted for the settings route.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/api frontend/src/stores frontend/src/components/settings frontend/src/views/SettingsView.vue
git commit -m "feat: configure custom AI providers"
```

---

### Task 6: Cached Vision Actions and “Eyes + Brain” Collaboration

**Files:**
- Create: `backend/src/main/java/com/smartdoc/ai/provider/VisionCompletionRequest.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionAction.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionActionRequest.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionActionResponse.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionObservation.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionCacheRecord.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionCacheMapper.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/PdfPageImageRenderer.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionActionService.java`
- Create: `backend/src/main/java/com/smartdoc/ai/vision/VisionActionController.java`
- Create: `backend/src/test/java/com/smartdoc/ai/vision/VisionActionServiceTest.java`
- Create: `backend/src/test/java/com/smartdoc/ai/vision/VisionActionIntegrationTest.java`
- Modify: `backend/src/main/java/com/smartdoc/ai/provider/OpenAiChatCompletionsAdapter.java`
- Modify: `backend/src/main/resources/schema.sql`
- Modify: `scripts/init_mysql.sql`
- Modify: `scripts/alter_ai_provider_config.sql`
- Modify: `frontend/src/api/providers.ts`
- Modify: `frontend/src/views/ReaderView.vue`
- Modify: `frontend/src/views/__tests__/ReaderView.spec.ts`

**Interfaces:**
- Produces: `POST /api/documents/{documentId}/vision-actions` as multipart data.
- Consumes: default text/vision routes, adapter, vault, file storage, document ownership, and quota.

- [ ] **Step 1: Write failing adapter and service tests**

```java
server.expect(jsonPath("$.messages[0].content[0].type").value("text"))
  .andExpect(jsonPath("$.messages[0].content[1].type").value("image_url"))
  .andExpect(jsonPath("$.messages[0].content[1].image_url.url").value(startsWith("data:image/png;base64,")));
```

Service tests must prove: another user's document is 404; page is 1-based and bounded; only one PDF page is rendered; unsupported MIME and decoded payload over 8 MiB return 400 before provider access; cache hit skips vision quota/provider; `DIRECT` calls only vision; `DEEP_ANALYSIS` calls vision once then text once; text failure retains the vision cache.

- [ ] **Step 2: Verify RED**

Run: `cd backend && mvn -q -Dtest=VisionActionServiceTest,VisionActionIntegrationTest,OpenAiChatCompletionsAdapterTest test`

Expected: compilation fails because vision contracts are absent.

- [ ] **Step 3: Add cache schema and deterministic cache key**

```sql
CREATE TABLE ai_vision_cache (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  document_id BIGINT NOT NULL,
  content_sha256 CHAR(64) NOT NULL,
  provider_id BIGINT NOT NULL,
  model VARCHAR(120) NOT NULL,
  prompt_version VARCHAR(40) NOT NULL,
  observation CLOB NOT NULL,
  created_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_ai_vision_cache UNIQUE (user_id,content_sha256,provider_id,model,prompt_version)
);
```

Use `TEXT` for MySQL. `VisionCacheMapper.selectOwned(...)` must bind `user_id` and `expires_at > now`. On a duplicate-key race, reread the winning row; do not use a JVM-only lock as the correctness mechanism.

- [ ] **Step 4: Render or validate exactly one image**

For a PDF page, use `PDDocument.load(InputStream)` and `PDFRenderer.renderImageWithDPI(pageNumber-1,144,ImageType.RGB)` inside try-with-resources; encode PNG after enforcing an output cap. For an uploaded screenshot, verify magic bytes with ImageIO, decode once, reject dimensions above 16 million pixels, normalize to PNG/JPEG, and never trust only the multipart content type.

- [ ] **Step 5: Implement structured observation and collaboration prompts**

The vision system prompt requires JSON with keys `description`, `ocrText`, `codeOrDiagram`, and `uncertainties`. Parse it with Jackson; if a compatible model wraps JSON in a code fence, remove only the outer fence. Reject blank/unparseable content with `INVALID_PROVIDER_RESPONSE` rather than saving it.

For `DEEP_ANALYSIS`, pass the saved observation plus the user's bounded question and current document text into the default text model. Limit the observation to 16,000 Unicode code points and the question to 500. Response metadata must disclose `visionModel`, optional `textModel`, and `visionCacheHit`.

- [ ] **Step 6: Add reader controls and transient screenshot payloads**

Add “识别当前页/图片” and “视觉识别后深度分析” actions only when a visual provider is configured. Show the exact models before submission. Use `FormData`; do not place Base64 in Pinia, localStorage, the route, logs, or question history. Clear pasted image blobs and revoke object URLs after success, failure, navigation, and component unmount.

- [ ] **Step 7: Run focused, full backend/frontend, and build verification**

Run: `cd backend && mvn -q -Dtest=VisionActionServiceTest,VisionActionIntegrationTest,OpenAiChatCompletionsAdapterTest test && mvn test`

Run: `cd frontend && pnpm test -- src/views/__tests__/ReaderView.spec.ts && pnpm test && pnpm build`

Expected: every command exits 0; mock servers receive no calls on validation/cache-hit cases.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/smartdoc/ai backend/src/test/java/com/smartdoc/ai backend/src/main/resources/schema.sql scripts frontend/src/api/providers.ts frontend/src/views/ReaderView.vue frontend/src/views/__tests__/ReaderView.spec.ts
git commit -m "feat: add cached visual document analysis"
```

---

### Task 7: GitHub Release, CI, and Secret Hygiene

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/secret-scan.yml`
- Modify: `.gitignore`
- Modify: `.env.example`
- Modify: `README.md`
- Create: `docs/provider-configuration.md`
- Create: `docs/screenshots/settings-providers.png`
- Create: `docs/screenshots/reader-vision.png`

**Interfaces:**
- Consumes: all completed application behavior.
- Produces: a safe, reproducible public repository; no runtime behavior beyond CI.

- [ ] **Step 1: Add failing documentation/secret contract checks**

Create a PowerShell-compatible test command in CI that fails if tracked files contain patterns such as `sk-[A-Za-z0-9]{20,}`, non-placeholder `AI_API_KEY=`, `Authorization: Bearer` followed by a literal token, or a populated master key. Exclude test fixtures only when they use explicit values such as `test-key-not-secret`.

Before adding the workflow, run the pattern scan and record any hit. If a real key is found, stop: revoke it first, then remove it from current files and Git history before any push.

- [ ] **Step 2: Create deterministic CI**

```yaml
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: {distribution: temurin, java-version: '11', cache: maven}
      - run: cd backend && mvn test
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with: {version: 10}
      - uses: actions/setup-node@v4
        with: {node-version: '20', cache: pnpm, cache-dependency-path: frontend/pnpm-lock.yaml}
      - run: cd frontend && pnpm install --frozen-lockfile
      - run: cd frontend && pnpm test && pnpm build
```

Add a separate secret-scan job for pushes and pull requests. CI must not define or request real AI provider keys.

- [ ] **Step 3: Write honest setup and compatibility documentation**

README must include: feature screenshots, architecture/data-flow diagram, H2 quick start, Docker MySQL/MinIO start, Demo account, Provider setup, compatibility boundary, and security model. `docs/provider-configuration.md` must provide example fields for DeepSeek and Qwen using placeholders only and explain that model availability/price must be checked with the provider.

Capture the two real screenshots from a local Demo/mock-provider run after removing visible keys, tokens, usernames beyond the demo account, local paths, and browser extensions. Do not commit placeholder images.

State exactly: “SmartDoc supports services implementing the OpenAI Chat Completions request/response shape used here; presets do not guarantee every model or vendor extension.”

- [ ] **Step 4: Verify clean clone behavior and migration warning**

From a temporary worktree or clean clone, run:

```powershell
cd backend
mvn test
cd ..\frontend
pnpm install --frozen-lockfile
pnpm test
pnpm build
```

Start the default local profile without any API key and verify Demo AI works. Do not execute MySQL migration automatically.

Required handoff text: `⚠️ MySQL 增量脚本已生成到 scripts/alter_ai_provider_config.sql；仅旧数据库需要手动执行，新数据库使用 scripts/init_mysql.sql。`

- [ ] **Step 5: Inspect Git history and repository state**

Run tracked-file and history scans, `git diff --check`, `git status --short`, and `git log --oneline --decorate -10`. Confirm `.env`, key files, H2 data, uploaded files, MinIO data, and `node_modules` are untracked/ignored.

- [ ] **Step 6: Commit**

```powershell
git add .github .gitignore .env.example README.md docs/provider-configuration.md docs/screenshots
git commit -m "docs: prepare SmartDoc for GitHub"
```

Do not push until the user supplies/approves the target GitHub repository and branch.

---

## Final Verification Gate

- [ ] Run `cd backend && mvn test` and record total tests, failures, errors, and skipped count.
- [ ] Run `cd frontend && pnpm test` and record files/tests passed.
- [ ] Run `cd frontend && pnpm build` and record exit status and bundle warnings.
- [ ] Run `git diff --check` and confirm no whitespace errors.
- [ ] Run secret scans against tracked files and full Git history; report exact non-secret fixture exceptions.
- [ ] Verify fresh H2 startup with Demo AI and no provider key.
- [ ] Verify one text mock provider, one vision mock provider, cache hit, text failure after vision success, and cross-user denial.
- [ ] Confirm no SQL was executed automatically and provide the manual migration reminder.

## Self-Review Result

- Spec coverage: Provider CRUD, domestic presets, text/vision routing, encryption/volatile fallback, quotas, cache, errors, tests, README, CI, and Git secret hygiene are each assigned to a task.
- Scope: Native Gemini/Anthropic/DashScope protocols, billing, streaming, audio/video, model discovery, and RAG remain excluded.
- Type consistency: `AiProviderProtocol`, `ProviderCapability`, `ProviderAdapter`, `ModelRouter`, `ProviderSecretVault`, and the REST DTO names are introduced before their consumers.
- Deferred-work scan: No incomplete implementation markers or unspecified error-handling steps remain.
