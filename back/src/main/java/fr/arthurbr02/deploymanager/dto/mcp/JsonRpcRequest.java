package fr.arthurbr02.deploymanager.dto.mcp;

import lombok.Data;
import java.util.Map;

@Data
public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private Map<String, Object> params;
}
