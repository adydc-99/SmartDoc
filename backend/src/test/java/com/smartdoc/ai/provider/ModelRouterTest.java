package com.smartdoc.ai.provider;
import org.junit.jupiter.api.Test; import java.util.*; import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class ModelRouterTest {
 @Test void refusesAnotherUsersConfiguredDefault() { AiProviderMapper providers=mock(AiProviderMapper.class); AiRoutingMapper routes=mock(AiRoutingMapper.class); AiRoutingConfig r=new AiRoutingConfig();r.setDefaultTextProviderId(7L);r.setDailyLimit(50);r.setMaxOutputTokens(128);when(routes.selectOwned(41)).thenReturn(r); ProviderSecretVault vault=new ProviderSecretVault(providers,Optional.empty()); ModelRouter router=new ModelRouter(providers,routes,vault,new ProviderAdapterRegistry(List.of())); ProviderHttpException e=assertThrows(ProviderHttpException.class,()->router.require(41,ProviderCapability.TEXT));assertEquals("MODEL_NOT_CONFIGURED",e.getCode());verify(providers).selectOwnedEnabled(7,41); }
}
