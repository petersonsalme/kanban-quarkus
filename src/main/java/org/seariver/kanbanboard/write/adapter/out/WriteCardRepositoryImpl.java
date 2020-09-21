package org.seariver.kanbanboard.write.adapter.out;

import org.seariver.kanbanboard.write.application.domain.Card;
import org.seariver.kanbanboard.write.application.domain.WriteCardRepository;
import org.seariver.kanbanboard.write.application.exception.DuplicatedDataException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.seariver.kanbanboard.write.application.exception.WriteException.Error.INVALID_DUPLICATED_DATA;

@ApplicationScoped
public class WriteCardRepositoryImpl implements WriteCardRepository {

    public static final String BUCKET_ID_FIELD = "bucket_id";
    public static final String EXTERNAL_ID = "external_id";
    public static final String POSITION_FIELD = "position";
    public static final String NAME_FIELD = "name";
    public static final String CREATED_AT_FIELD = "created_at";
    public static final String UPDATED_AT_FIELD = "updated_at";
    private static final String DESCRIPTION_FIELD = "description";
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public WriteCardRepositoryImpl(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public void create(Card card) {
        try {
            var sql = "INSERT INTO card (bucket_id, external_id, position, name) " +
                    "values (:bucket_id, :external_id, :position, :name)";

            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue(BUCKET_ID_FIELD, card.getBucketId())
                    .addValue(EXTERNAL_ID, card.getCardExternalId())
                    .addValue(POSITION_FIELD, card.getPosition())
                    .addValue(NAME_FIELD, card.getName());

            jdbcTemplate.update(sql, parameters);

        } catch (DuplicateKeyException exception) {

            var duplicatedException = new DuplicatedDataException(INVALID_DUPLICATED_DATA, exception);

            List<Card> existentCards = findByExternalIdOrPosition(card.getCardExternalId(), card.getPosition());

            existentCards.forEach(existentCard -> {
                if (existentCard.getCardExternalId().equals(card.getCardExternalId())) {
                    duplicatedException.addError("id", card.getCardExternalId());
                }

                if (existentCard.getPosition() == card.getPosition()) {
                    duplicatedException.addError("position", card.getPosition());
                }
            });

            throw duplicatedException;
        }
    }

    @Override
    public void update(Card card) {

        var sql = "UPDATE card SET position = :position, name = :name, description = :description WHERE external_id = :external_id";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue(EXTERNAL_ID, card.getCardExternalId())
                .addValue(POSITION_FIELD, card.getPosition())
                .addValue(NAME_FIELD, card.getName())
                .addValue(DESCRIPTION_FIELD, card.getDescription());

        jdbcTemplate.update(sql, parameters);
    }

    @Override
    public Optional<Card> findByExternalId(UUID externalId) {

        var sql = "SELECT bucket_id, external_id, position, name, description, created_at, updated_at FROM card WHERE external_id = :external_id";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue(EXTERNAL_ID, externalId);

        return jdbcTemplate.query(sql, parameters, resultSet -> {

            if (resultSet.next()) {
                return Optional.of(new Card()
                        .setBucketId(resultSet.getLong(BUCKET_ID_FIELD))
                        .setCardExternalId(UUID.fromString(resultSet.getString(EXTERNAL_ID)))
                        .setPosition(resultSet.getDouble(POSITION_FIELD))
                        .setName(resultSet.getString(NAME_FIELD))
                        .setDescription(resultSet.getString(DESCRIPTION_FIELD))
                        .setCreatedAt(resultSet.getTimestamp(CREATED_AT_FIELD).toLocalDateTime())
                        .setUpdatedAt(resultSet.getTimestamp(UPDATED_AT_FIELD).toLocalDateTime())
                );
            }

            return Optional.empty();
        });
    }

    private List<Card> findByExternalIdOrPosition(UUID externalId, double position) {

        var sql = "SELECT bucket_id, external_id, position, name, created_at, updated_at " +
                "FROM card WHERE external_id = :external_id OR position = :position";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue(EXTERNAL_ID, externalId)
                .addValue(POSITION_FIELD, position);

        return jdbcTemplate.query(sql, parameters, (rs, rowNum) ->
                new Card()
                        .setBucketId(rs.getLong(BUCKET_ID_FIELD))
                        .setCardExternalId(UUID.fromString(rs.getString(EXTERNAL_ID)))
                        .setPosition(rs.getDouble(POSITION_FIELD))
                        .setName(rs.getString(NAME_FIELD))
                        .setCreatedAt(rs.getTimestamp(CREATED_AT_FIELD).toLocalDateTime())
                        .setUpdatedAt(rs.getTimestamp(UPDATED_AT_FIELD).toLocalDateTime())
        );
    }
}
