import Utils.SecurityUtils;

public class TestAdminLogin {
    public static void main(String[] args) {
        // Test password hashing
        String password = "123456";
        String salt = "V2NbeX+IdYWWEO2Sz/IZyw==";
        
        String hash = SecurityUtils.hashPassword(password, salt);
        String expectedHash = "0SKuatdntt22eBHDTnUjJ0wOWzIL1HWoUr9786ng99g=";
        
        System.out.println("Password: " + password);
        System.out.println("Salt: " + salt);
        System.out.println("Generated Hash: " + hash);
        System.out.println("Expected Hash: " + expectedHash);
        System.out.println("Match: " + (hash != null && hash.equals(expectedHash)));
    }
}
