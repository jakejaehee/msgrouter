package msgrouter.api.security;

import msgrouter.engine.MsgRouter;

public class Decryptor {
	private EncryptUtil decryptor = null;

	public Decryptor(String encryptName) {
		this.decryptor = MsgRouter.getInstance().getDecryptor(encryptName);
	}

	public byte[] decrypt(byte[] bytes, int offset, int length) {
		if (decryptor == null) {
			return bytes;
		}
		return decryptor.doFinal(bytes, offset, length);
	}
}
