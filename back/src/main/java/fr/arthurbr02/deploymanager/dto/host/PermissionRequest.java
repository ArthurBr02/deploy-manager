package fr.arthurbr02.deploymanager.dto.host;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record PermissionRequest(@NotNull UUID hostId, boolean canDeploy, boolean canEdit, boolean canExecute, boolean canDump) {}
