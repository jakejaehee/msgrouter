package msgrouter.api.security;

import msgrouter.engine.MsgRouter;

public class Encryptor {
	private EncryptUtil encryptor = null;

	public Encryptor(String encryptName) {
		this.encryptor = MsgRouter.getInstance().getEncryptor(encryptName);
	}

	public byte[] encrypt(byte[] bytes, int offset, int length) {
		if (encryptor == null) {
			return bytes;
		}
		return encryptor.doFinal(bytes, offset, length);
	}
}
