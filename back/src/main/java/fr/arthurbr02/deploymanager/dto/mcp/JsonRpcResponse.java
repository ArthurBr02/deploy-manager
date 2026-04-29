package fr.arthurbr02.deploymanager.dto.mcp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonRpcResponse {
    private String jsonrpc;
    private Object id;
    private Object result;
    private Object error;
}
