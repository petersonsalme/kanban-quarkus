package org.seariver.kanbanboard.write.domain.application;

import java.util.UUID;

public class UpdateBucketCommand implements Command {

    private final UUID uuid;
    private final double position;
    private final String name;

    public UpdateBucketCommand(UUID uuid, double position, String name) {
        this.uuid = uuid;
        this.position = position;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }
}
