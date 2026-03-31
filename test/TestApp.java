package test;

/**
 * Тестовый класс для проверки обфускатора
 */
public class TestApp {
    
    private String secretKey = "my-secret-key-12345";
    private int counter = 0;
    
    public static void main(String[] args) {
        TestApp app = new TestApp();
        app.run();
    }
    
    public void run() {
        System.out.println("=== Test Application ===");
        System.out.println("Secret: " + getSecret());
        
        for (int i = 0; i < 5; i++) {
            process(i);
        }
        
        System.out.println("Counter: " + counter);
        System.out.println("=== Done ===");
    }
    
    private String getSecret() {
        return "DECRYPTED: " + secretKey;
    }
    
    private void process(int value) {
        counter++;
        String message = "Processing value: " + value;
        System.out.println(message);
        
        if (value % 2 == 0) {
            System.out.println("Even number");
        } else {
            System.out.println("Odd number");
        }
    }
    
    private int calculate(int a, int b) {
        int sum = a + b;
        int product = a * b;
        return sum + product;
    }
}
