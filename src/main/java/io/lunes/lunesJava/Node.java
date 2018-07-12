package io.lunes.lunesJava;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Node {
    private static final String DEFAULT_NODE = "https://lunesnode.lunes.io";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TX_INFO = new TypeReference<Map<String, Object>>() {
    };

    private final URI uri;
    private final CloseableHttpClient client = HttpClients.custom()
            .setDefaultRequestConfig(
                    RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
            .build();

    public Node() {
        try {
            this.uri = new URI(DEFAULT_NODE);
        } catch (URISyntaxException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    public Node(String uri) throws URISyntaxException {
        this.uri = new URI(uri);
    }

    private static <T> T parse(HttpResponse r, TypeReference<T> ref) throws IOException {
        return mapper.readValue(r.getEntity().getContent(), ref);
    }

    private static JsonNode parse(HttpResponse r, String... keys) throws IOException {
        JsonNode tree = mapper.readTree(r.getEntity().getContent());
        for (String key : keys) {
            tree = tree.get(key);
        }
        return tree;
    }

    public String getVersion() throws IOException {
        return send("/node/version", "version").asText();
    }

    public int getHeight() throws IOException {
        return send("/blocks/height", "height").asInt();
    }

    public long getBalance(String address) throws IOException {
        return send("/addresses/balance/" + address, "balance").asLong();
    }

    public long getBalance(String address, int confirmations) throws IOException {
        return send("/addresses/balance/" + address + "/" + confirmations, "balance").asLong();
    }

    public long getBalance(String address, String assetId) throws IOException {
        return Asset.isLunes(assetId)
                ? getBalance(address)
                : send("/assets/balance/" + address + "/" + assetId, "balance").asLong();
    }

    /**
     * Returns transaction by its ID.
     *
     * @param txId transaction ID
     * @return transaction object
     * @throws IOException if no transaction with the given ID exists
     */
    public Map<String, Object> getTransaction(String txId) throws IOException {
        return mapper.convertValue(send("/transactions/info/" + txId), TX_INFO);
    }

    /**
     * Returns block at given height.
     *
     * @param height blockchain height
     * @return block object
     * @throws IOException if no block exists at the given height
     */
    public Block getBlock(int height) throws IOException {
        return mapper.convertValue(send("/blocks/at/" + height), Block.class);
    }

    /**
     * Returns block by its signature.
     *
     * @param signature block signature
     * @return block object
     * @throws IOException if no block with the given signature exists
     */
    public Block getBlock(String signature) throws IOException {
        return mapper.convertValue(send("/blocks/signature/" + signature), Block.class);
    }

    public boolean validateAddresses(String address) throws IOException {
        return send("/addresses/validate/" + address, "valid").asBoolean();
    }

    public String getAddrByAlias(String alias) throws IOException {
        return send("/addresses/alias/by-alias/" + alias, "address").textValue();
    }

    /**
     * Sends a signed transaction and returns its ID.
     *
     * @param tx signed transaction (as created by static methods in Transaction class)
     * @return Transaction ID
     * @throws IOException
     */
    public String send(Transaction tx) throws IOException {
        return parse(exec(request(tx)), "id").asText();
    }

    private JsonNode send(String path, String... key) throws IOException {
        return parse(exec(request(path)), key);
    }

    public String transfer(PrivateKeyAccount from, String recipient, long amount, long fee) throws IOException {
        Transaction tx = Transaction.makeTransferTx(from, recipient, amount, null, fee, null);
        return send(tx);
    }

    public String transfer(PrivateKeyAccount from, String assetId, String recipient,
                           long amount, long fee, String feeAssetId) throws IOException {
        Transaction tx = Transaction.makeTransferTx(from, recipient, amount, assetId, fee, feeAssetId);
        return send(tx);
    }

    public String lease(PrivateKeyAccount from, String recipient, long amount, long fee) throws IOException {
        Transaction tx = Transaction.makeLeaseTx(from, recipient, amount, fee);
        return send(tx);
    }

    public String cancelLease(PrivateKeyAccount account, byte chainId, String txId, long fee) throws IOException {
        Transaction tx = Transaction.makeLeaseCancelTx(account, chainId, txId, fee);
        return send(tx);
    }

    public String issueAsset(PrivateKeyAccount account, byte chainId, String name, String description, long quantity,
                             byte decimals, boolean reissuable, String script, long fee) throws IOException {
        Transaction tx = Transaction.makeIssueTx(account, chainId, name, description, quantity, decimals, reissuable, script, fee);
        return send(tx);
    }

    public String reissueAsset(PrivateKeyAccount account, byte chainId, String assetId, long quantity, boolean reissuable, long fee) throws IOException {
        Transaction tx = Transaction.makeReissueTx(account, chainId, assetId, quantity, reissuable, fee);
        return send(tx);
    }

    public String burnAsset(PrivateKeyAccount account, byte chainId, String assetId, long amount, long fee) throws IOException {
        Transaction tx = Transaction.makeBurnTx(account, chainId, assetId, amount, fee);
        return send(tx);
    }

    public String alias(PrivateKeyAccount account, byte chainId, String alias, long fee) throws IOException {
        Transaction tx = Transaction.makeAliasTx(account, alias, chainId, fee);
        return send(tx);
    }



    private <T> HttpUriRequest request(String path, String... headers) {
        HttpUriRequest req = new HttpGet(uri.resolve(path));
        for (int i = 0; i < headers.length; i += 2) {
            req.addHeader(headers[i], headers[i + 1]);
        }
        return req;
    }

    private HttpUriRequest request(Transaction tx) {
        HttpPost request = new HttpPost(uri.resolve(tx.endpoint));
        request.setEntity(new StringEntity(tx.getJson(), ContentType.APPLICATION_JSON));
        return request;
    }

    private HttpResponse exec(HttpUriRequest request) throws IOException {
        HttpResponse r = client.execute(request);
        if (r.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            try {
                throw new IOException(EntityUtils.toString(r.getEntity()));
            } catch (JsonParseException e) {
                throw new RuntimeException("Server error " + r.getStatusLine().getStatusCode());
            }
        }
        return r;
    }
}
