package com.smartdoc.ai.provider; public interface SecretCipher { String encrypt(String plain,String aad); String decrypt(String ciphertext,String aad); }
