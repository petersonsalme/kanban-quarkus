package org.seariver.kanbanboard.write.domain.application;

import org.seariver.kanbanboard.write.domain.core.Bucket;
import org.seariver.kanbanboard.write.domain.core.WriteBucketRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named
@ApplicationScoped
public class CreateBucketHandler implements Handler<CreateBucketCommand> {

    private final WriteBucketRepository repository;

    public CreateBucketHandler(WriteBucketRepository repository) {
        this.repository = repository;
    }

    public void handle(CreateBucketCommand command) {

        var bucket = new Bucket()
                .setBucketExternalId(command.getBucketExternalId())
                .setPosition(command.getPosition())
                .setName(command.getName());

        repository.create(bucket);
    }
}
