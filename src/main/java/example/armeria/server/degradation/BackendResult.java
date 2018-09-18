package example.armeria.server.degradation;

import com.linecorp.armeria.common.AggregatedHttpMessage;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;

import java.util.Optional;

public class BackendResult {
    private final boolean isSuccess;
    private final Optional<String> value;

    public BackendResult(boolean isSuccess, Optional<String> value) {
        this.isSuccess = isSuccess;
        this.value = value;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Optional<String> getValue() {
        return value;
    }
}
