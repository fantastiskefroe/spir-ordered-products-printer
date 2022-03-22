package dk.fantastiskefroe.spir.orderedproductsprinter;

import net.schmizz.sshj.xfer.InMemorySourceFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class InMemoryFileFromString extends InMemorySourceFile {
    private byte[] data;
    private int i = 0;
    public InMemoryFileFromString(String fileContents) {
        data = fileContents.getBytes(StandardCharsets.UTF_8);
    }
    public InMemoryFileFromString(ByteArrayOutputStream os) {
        data = os.toByteArray();
    }

    @Override
    public String getName() {
        return "filename";
    }

    @Override
    public long getLength() {
        return data.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }
}
