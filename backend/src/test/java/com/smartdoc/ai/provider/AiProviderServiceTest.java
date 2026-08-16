package com.smartdoc.ai.provider;

import com.smartdoc.ai.DailyAiQuota;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiProviderServiceTest {
    @Test void disabledProviderTestDoesNotConsumeQuotaOrProbe() {
        AiProviderMapper providers=mock(AiProviderMapper.class);AiRoutingMapper routes=mock(AiRoutingMapper.class);
        AiProviderConfig p=AiProviderConfig.textProvider(7,"off","CUSTOM","https://example.cn/v1","m");p.setId(5L);p.setEnabled(false);
        when(providers.selectOwned(5,7)).thenReturn(p);when(providers.clearEncryptedKeyOwned(anyLong(),anyLong(),any())).thenReturn(1);
        DailyAiQuota quota=new DailyAiQuota(Clock.systemUTC());ProviderSecretVault vault=new ProviderSecretVault(providers,Optional.empty());
        ProviderAdapter adapter=mock(ProviderAdapter.class);when(adapter.protocol()).thenReturn(AiProviderProtocol.OPENAI_CHAT_COMPLETIONS);
        ModelRouter router=new ModelRouter(providers,routes,vault,new ProviderAdapterRegistry(List.of(adapter)));
        AiProviderService service=new AiProviderService(providers,routes,vault,new ProviderUrlPolicy(false,false),router,quota);
        assertThrows(ProviderHttpException.class,()->service.test(7,5));assertEquals(0,quota.used(7));verify(adapter,never()).probe(any(),anyString());
    }

    @Test void enabledVisionOnlyProviderCanRunConnectionProbe() {
        AiProviderMapper providers=mock(AiProviderMapper.class);AiRoutingMapper routes=mock(AiRoutingMapper.class);
        AiProviderConfig p=AiProviderConfig.textProvider(7,"vision","CUSTOM","https://example.cn/v1","vision-model");
        p.setId(6L);p.setEnabled(true);p.setSupportsText(false);p.setSupportsVision(true);
        when(providers.selectOwned(6,7)).thenReturn(p);
        ProviderSecretVault vault=mock(ProviderSecretVault.class);when(vault.load(7,6,p)).thenReturn(Optional.of("vision-key"));
        ProviderAdapter adapter=mock(ProviderAdapter.class);when(adapter.protocol()).thenReturn(AiProviderProtocol.OPENAI_CHAT_COMPLETIONS);
        ModelRouter router=new ModelRouter(providers,routes,vault,new ProviderAdapterRegistry(List.of(adapter)));
        DailyAiQuota quota=new DailyAiQuota(Clock.systemUTC());AiRoutingConfig routing=new AiRoutingConfig();routing.setDailyLimit(10);when(routes.selectOwned(7)).thenReturn(routing);
        AiProviderService service=new AiProviderService(providers,routes,vault,new ProviderUrlPolicy(false,false),router,quota);

        Map<String,Object> result=service.test(7,6);

        assertEquals(true,result.get("success"));assertEquals(1,quota.used(7));verify(adapter).probe(p,"vision-key");
    }
}
