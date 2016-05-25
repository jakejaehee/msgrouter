package msgrouter.api.security;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtil {
	public static final int BLOCK_SIZE_SEED = 16;

	public static final String ENCRYPT_ALGORITHM_SEED = "SEED";
	public static final String ENCRYPT_ALGORITHM_DES = "DES";
	public static final String ENCRYPT_ALGORITHM_DES_EDE = "DESede";
	public static final String ENCRYPT_ALGORITHM_TRIPLE_DES = "TripleDES";
	public static final String ENCRYPT_ALGORITHM_AES = "AES";

	public static final String PADDING_PKCS5 = "PKCS5";
	public static final String PADDING_ANSI_X923 = "ANSI-X.923";

	public static final String OPERATION_MODE_ECB = "ECB";
	public static final String OPERATION_MODE_CBC = "CBC";

	private final String algorithm;
	private final String operationMode;
	private final String padding;
	private final int encryptOrDecrypt;
	private Key key = null;
	private int[] seedKey = null;
	private Cipher cipher = null;

	private EncryptUtil() {
		this.algorithm = null;
		this.operationMode = null;
		this.padding = null;
		this.encryptOrDecrypt = -1;
	}

	private EncryptUtil(String algorithm, String operationMode, String padding,
			byte[] userKeyBytes, byte[] iv, int encryptOrDecrypt) {
		try {
			this.algorithm = algorithm;
			this.operationMode = operationMode;
			this.padding = padding;
			this.encryptOrDecrypt = encryptOrDecrypt;

			if (ENCRYPT_ALGORITHM_SEED.equals(algorithm)) {
				this.seedKey = (int[]) generateKey(algorithm, userKeyBytes);
			} else {
				if (OPERATION_MODE_ECB.equals(operationMode)) {
					this.key = (Key) generateKey(algorithm, userKeyBytes);
					this.cipher = javax.crypto.Cipher.getInstance(algorithm
							+ "/" + operationMode + "/" + PADDING_PKCS5);
					this.cipher.init(encryptOrDecrypt, key);
				} else if (OPERATION_MODE_CBC.equals(operationMode)) {
					this.key = (Key) generateKey(algorithm, userKeyBytes);
					this.cipher = javax.crypto.Cipher.getInstance(algorithm
							+ "/" + operationMode + "/" + PADDING_PKCS5);
					this.cipher
							.init(encryptOrDecrypt,
									key,
									new IvParameterSpec(iv != null
											&& iv.length > 0 ? iv : createIV(
											algorithm, operationMode)));
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public static EncryptUtil newEncryptor(String algorithm,
			String operationMode, String padding, byte[] userKeyBytes, byte[] iv) {
		EncryptUtil encryptor = new EncryptUtil(algorithm, operationMode,
				padding, userKeyBytes, iv, javax.crypto.Cipher.ENCRYPT_MODE);
		return encryptor;
	}

	public static EncryptUtil newDecryptor(String algorithm,
			String operationMode, String padding, byte[] userKeyBytes, byte[] iv) {
		EncryptUtil encryptor = new EncryptUtil(algorithm, operationMode,
				padding, userKeyBytes, iv, javax.crypto.Cipher.DECRYPT_MODE);
		return encryptor;
	}

	public static EncryptUtil newSeedEncryptor(String padding,
			byte[] userKeyBytes) {
		EncryptUtil encryptor = new EncryptUtil(ENCRYPT_ALGORITHM_SEED, null,
				padding, userKeyBytes, null, javax.crypto.Cipher.ENCRYPT_MODE);
		return encryptor;
	}

	public static EncryptUtil newSeedDecryptor(String padding,
			byte[] userKeyBytes) {
		EncryptUtil encryptor = new EncryptUtil(ENCRYPT_ALGORITHM_SEED, null,
				padding, userKeyBytes, null, javax.crypto.Cipher.DECRYPT_MODE);
		return encryptor;
	}

	public static Object generateKey(String algorithm, byte[] userKeyBytes) {
		try {
			if (ENCRYPT_ALGORITHM_DES.equals(algorithm)) {
				KeySpec keySpec = new DESKeySpec(userKeyBytes);
				SecretKeyFactory secretKeyFactory = SecretKeyFactory
						.getInstance(algorithm);
				SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
				return secretKey;
			} else if (ENCRYPT_ALGORITHM_DES_EDE.equals(algorithm)
					|| ENCRYPT_ALGORITHM_TRIPLE_DES.equals(algorithm)) {
				KeySpec keySpec = new DESedeKeySpec(to24BytesKey(userKeyBytes));
				SecretKeyFactory secretKeyFactory = SecretKeyFactory
						.getInstance(algorithm);
				SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
				return secretKey;
			} else if (ENCRYPT_ALGORITHM_SEED.equals(algorithm)) {
				int pdwRoundKey[] = new int[32];
				SEED128_KISA.SeedRoundKey(pdwRoundKey, userKeyBytes);
				return pdwRoundKey;
			} else {
				SecretKeySpec keySpec = new SecretKeySpec(userKeyBytes,
						algorithm);
				return keySpec;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public byte[] doFinal(byte[] bytes) {
		return doFinal(bytes, 0, bytes.length);
	}

	public byte[] doFinal(byte[] src, int offset, int length) {
		try {
			byte[] finalBytes = null;
			byte[] targetSrcNotPadded = null;
			byte[] targetSrc = null;
			if (offset != 0 || src.length != length) {
				targetSrcNotPadded = new byte[length];
				System.arraycopy(src, offset, targetSrcNotPadded, 0, length);
			} else {
				targetSrcNotPadded = src;
			}

			if (encryptOrDecrypt == javax.crypto.Cipher.ENCRYPT_MODE) {
				targetSrc = padding != null ? addPadding(targetSrcNotPadded)
						: targetSrcNotPadded;
			} else {
				targetSrc = targetSrcNotPadded;
			}
			if (ENCRYPT_ALGORITHM_SEED.equals(algorithm)) {
				finalBytes = new byte[targetSrc.length];
				int rt = targetSrc.length / BLOCK_SIZE_SEED;
				switch (encryptOrDecrypt) {
				case javax.crypto.Cipher.ENCRYPT_MODE:
					for (int j = 0; j < rt; j++) {
						byte sSource[] = new byte[BLOCK_SIZE_SEED];
						byte sTarget[] = new byte[BLOCK_SIZE_SEED];
						System.arraycopy(targetSrc, (j * BLOCK_SIZE_SEED),
								sSource, 0, BLOCK_SIZE_SEED);

						SEED128_KISA.SeedEncrypt(sSource, seedKey, sTarget);
						System.arraycopy(sTarget, 0, finalBytes,
								(j * BLOCK_SIZE_SEED), sTarget.length);
					}
					break;
				case javax.crypto.Cipher.DECRYPT_MODE:
					byte sSource[] = new byte[BLOCK_SIZE_SEED];
					byte sTarget[] = new byte[BLOCK_SIZE_SEED];
					for (int j = 0; j < rt; j++) {
						System.arraycopy(targetSrc, (j * BLOCK_SIZE_SEED),
								sSource, 0, BLOCK_SIZE_SEED);
						SEED128_KISA.SeedDecrypt(sSource, seedKey, sTarget);
						System.arraycopy(sTarget, 0, finalBytes,
								(j * BLOCK_SIZE_SEED), BLOCK_SIZE_SEED);
					}
					finalBytes = padding != null ? removePadding(finalBytes)
							: finalBytes;
					break;
				}
			} else {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CipherOutputStream cos = new CipherOutputStream(baos, cipher);
				for (int i = 0; i < targetSrc.length; i++) {
					cos.write(targetSrc[i]);
				}
				cos.close();
				finalBytes = baos.toByteArray();
			}
			return finalBytes;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public String doFinal(String str) {
		return doFinal(str, null);
	}

	public String doFinal(String str, String charset) {
		String dst = null;
		try {
			switch (encryptOrDecrypt) {
			case javax.crypto.Cipher.ENCRYPT_MODE:
				byte[] inBytes = charset != null ? str.getBytes(charset) : str
						.getBytes();
				byte[] encBytes = doFinal(inBytes);
				dst = bytesToHexString(encBytes);
				break;
			case javax.crypto.Cipher.DECRYPT_MODE:
				byte[] decBytes = doFinal(hexStringToBytes(str));
				dst = charset != null ? new String(decBytes, charset)
						: new String(decBytes);
				break;
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

		return dst;
	}

	public byte[] createIV(String algorithm, String mode) {
		if (ENCRYPT_ALGORITHM_AES.equals(algorithm)) {
			return createIV(16, (byte) 0);
		} else if (ENCRYPT_ALGORITHM_DES.equals(algorithm)
				|| ENCRYPT_ALGORITHM_DES_EDE.equals(algorithm)
				|| ENCRYPT_ALGORITHM_TRIPLE_DES.equals(algorithm)) {
			return createIV(8, (byte) 0);
		} else {
			return createIV(8, (byte) 0);
		}
	}

	public static byte[] createIV(int size, byte initValue) {
		byte[] iv = new byte[size];
		for (int i = 0; i < iv.length; i++) {
			iv[i] = initValue;
		}
		return iv;
	}

	public static byte[] to24BytesKey(byte[] key) {
		byte[] newKey = new byte[24];
		System.arraycopy(key, 0, newKey, 0, 16);
		System.arraycopy(key, 0, newKey, 16, 8);
		return newKey;
	}

	public static byte[] hexStringToBytes(String hexStr) {
		if (hexStr == null || "".equals(hexStr)) {
			return new byte[0];
		}
		byte[] bytes = new byte[hexStr.length() / 2];
		for (int i = 0, j = 0; i < hexStr.length(); i += 2) {
			bytes[j++] = (byte) Integer
					.parseInt(hexStr.substring(i, i + 2), 16);
		}
		return bytes;
	}

	public static String bytesToHexString(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(Integer.toString((b & 0xF0) >> 4, 16));
			sb.append(Integer.toString(b & 0x0F, 16));
		}
		return sb.toString();
	}

	public static byte[] loadKeyFromHexStringFile(String filepath)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(filepath)));
		String hexStr = reader.readLine().trim();
		return hexStringToBytes(hexStr);
	}

	public static byte[] loadKeyFromBinaryFile(String filepath)
			throws IOException {
		FileInputStream fis = new FileInputStream(filepath);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int i = 0;
		while ((i = fis.read()) != -1) {
			baos.write(i);
		}
		fis.close();
		baos.close();
		return baos.toByteArray();
	}

	public static byte[] randomIV(int size) {
		SecureRandom random = new SecureRandom();
		byte[] iv = new byte[size];
		random.nextBytes(iv);
		return iv;
	}

	private static String bytesToStr(byte[] bytes) {
		return bytesToHexString(bytes);
		// return new sun.misc.BASE64Encoder().encode(bytes);
	}

	private static byte[] strToBytes(String str) throws IOException {
		return hexStringToBytes(str);
		// return new sun.misc.BASE64Decoder().decodeBuffer(str);
	}

	public byte[] addPadding(byte[] src) {
		if (padding == null) {
			return src;
		}
		if (padding.startsWith(PADDING_ANSI_X923)) {
			return addAnsiX923Padding(src, BLOCK_SIZE_SEED);
		} else if (padding.startsWith(PADDING_PKCS5)) {
			return addPKCS5Padding(src);
		} else {
			return src;
		}
	}

	public byte[] removePadding(byte[] src) {
		if (padding == null) {
			return src;
		}
		if (padding.startsWith(PADDING_ANSI_X923)) {
			return removeAnsiX923Padding(src, BLOCK_SIZE_SEED);
		} else if (padding.startsWith(PADDING_PKCS5)) {
			return removePKCS5Padding(src);
		} else {
			return src;
		}
	}

	public static byte[] addPKCS5Padding(byte[] src) {
		byte[] dst = null;
		int blockCnt = src.length / 16 + 1;
		int padValue = 16 - src.length % 16;
		if (padValue == 0) {
			dst = new byte[blockCnt * 16];
			System.arraycopy(src, 0, dst, 0, src.length);
			for (int i = src.length; i < dst.length; i++) {
				dst[i] = (byte) 0x10;
			}
		} else {
			dst = new byte[blockCnt * 16];
			System.arraycopy(src, 0, dst, 0, src.length);
			for (int i = src.length; i < dst.length; i++) {
				dst[i] = (byte) padValue;
			}
		}
		// return dst;
		return src;
	}

	public static byte[] removePKCS5Padding(byte[] src) {
		return src;
	}

	public static byte[] addAnsiX923Padding(byte[] src, int blockSize) {
		int paddingCnt = src.length % blockSize;
		byte[] paddingResult = null;

		if (paddingCnt != 0) {
			paddingResult = new byte[src.length + (blockSize - paddingCnt)];
			System.arraycopy(src, 0, paddingResult, 0, src.length);
			int addPaddingCnt = blockSize - paddingCnt;
			for (int i = 0; i < addPaddingCnt; i++) {
				paddingResult[src.length + i] = 0x00;
			}
			paddingResult[paddingResult.length - 1] = (byte) addPaddingCnt;
		} else {
			paddingResult = src;
		}

		return paddingResult;
	}

	public static byte[] removeAnsiX923Padding(byte[] src, int blockSize) {
		if (src == null || src.length == 0) {
			return src;
		}

		byte[] unpadding = null;
		boolean isPadding = false;

		int paddingSize = src[src.length - 1];
		// if (paddingSize < 0) {
		// throw new RuntimeException(
		// "Padding information error: The padding size should not be negative: "
		// + paddingSize);
		// }
		if (paddingSize > 0 && paddingSize < blockSize) {
			for (int i = src.length - paddingSize; i < src.length - 1; i++) {
				if (src[i] != 0x00) {
					isPadding = false;
				}
				break;
			}
			isPadding = true;
		} else {
			isPadding = false;
		}
		if (isPadding) {
			unpadding = new byte[src.length - paddingSize];
			if (src.length < paddingSize) {
				throw new RuntimeException(
						"Padding information error: The padding size should not be bigger than source size: source size="
								+ src.length + ", padding size=" + paddingSize);
			}
			System.arraycopy(src, 0, unpadding, 0, unpadding.length);
		} else {
			unpadding = src;
		}
		return unpadding;
	}
}
