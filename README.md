# lunesJava
Lunes blockchain Java library. 


## Using lunesJava in your project
```java
 import io.lunes.lunesJava.*;

 import java.io.IOException;
 import java.net.URISyntaxException;

 import static io.lunes.lunesJava.Asset.LUNES;

 public class Example {

    public static void main(String[] args) throws IOException, URISyntaxException {
        final long FEE = 100000;
        
        // Create signing mainnet account
        String seed = "health lazy lens fix dwarf salad breeze myself silly december endless rent faculty report beyond";
        PrivateKeyAccount alice = PrivateKeyAccount.fromSeed(seed, 0, Account.MAINNET);
        PrivateKeyAccount alice = PrivateKeyAccount.fromPrivateKey(privateKey, Account.MAINNET);
        PrivateKeyAccount alice = PrivateKeyAccount.fromSeedHash(seedHash, Account.MAINNET);
        // Retrieve its public key
        byte[] publicKey = alice.getPublicKey();
        // and its address
        String address = alice.getAddress();

        // Create a Node ("https://lunesnode.lunes.io" by default, or you can pass local node here)
        Node node = new Node("https://127.0.0.1:5555");

        // Get blockchain height
        int height = node.getHeight();
        System.out.println("height: " + height);

        // Learn address balance
        System.out.println("Alice's balance: " + node.getBalance(address));

        // Transactions, 8 long (EX: 10 lunes = 10 00000000)
        String bob = "3N9gDFq8tKFhBDBTQx3zqvtpXjw5wW3syA";
        String txId = node.transfer(account, bob, 1000000000, 100000);
			
        // Leasing coins
        String leaseTxId = node.lease(alice, bob, 100 * Asset.TOKEN, FEE);
        // Canceling a lease by tx ID
        String cancelTxId = node.cancelLease(alice, Account.MAINNET, leaseTxId, FEE);

    }
 }
```
## Building the library

To build from scratch, run

```
mvn clean package
```

The outputs are placed under the `target` directory.
