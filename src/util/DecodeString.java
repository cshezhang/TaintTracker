package util;

public class DecodeString {
    public static void main(String[] args) {
        System.out.println(new DecodeString().ishumeiDecode("9c8d9a9e8b9adf8d9a8b8a8d91df8c92969bdf9a928f8b86"));
    }
    public String ishumeiDecode(String input) {
        String res = m55003g(input);
        return res;
    }

    public static String m55003g(String str) {
        return new String(m54997a(m55002f(str)));
    }

    public static byte[] m55002f(String str) {
        byte[] bytes = str.getBytes();
        int length = bytes.length;
        byte[] bArr = new byte[(length / 2)];
        for (int i = 0; i < length; i += 2) {
            bArr[i / 2] = (byte) Integer.parseInt(new String(bytes, i, 2), 16);
        }
        return bArr;
    }
    public static byte[] m54997a(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length];
        for (int i = 0; i < bArr.length; i++) {
            bArr2[i] = (byte) (bArr[i] ^ -1);
        }
        return bArr2;
    }

}
