package com.clara.ops.challenge.document_management_service_challenge.repository;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.DocumentSearchFilters;
import com.clara.ops.challenge.document_management_service_challenge.entity.Document;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory for JPA {@link Specification} objects used to filter {@link Document} queries
 * dynamically. All filter fields are optional; absent or blank values are silently ignored.
 *
 * <p>Each filter criterion is built by a dedicated private helper returning an {@link Optional}
 * predicate, keeping the main composition method readable and each rule independently testable.
 */
public class DocumentSpecification {

    private DocumentSpecification() {}

    /**
     * Builds a {@link Specification} that applies every non-null filter present in {@code filters}.
     *
     * <ul>
     *   <li><b>user</b> — exact match on the {@code username} column.
     *   <li><b>fileName</b> — case-insensitive {@code LIKE %value%} match.
     *   <li><b>tags</b> — documents that contain <em>at least one</em> of the provided tags (inner
     *       join with the {@code document_tags} collection table).
     * </ul>
     *
     * @param filters the search criteria supplied by the caller
     * @return a {@link Specification} combining all active predicates with {@code AND}; results are
     *     marked {@code DISTINCT} when a tag join is required to avoid duplicate rows
     */
    public static Specification<Document> withFilters(DocumentSearchFilters filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            userPredicate(root, cb, filters.user()).ifPresent(predicates::add);
            namePredicate(root, cb, filters.fileName()).ifPresent(predicates::add);
            tagsPredicate(root, query, filters.tags()).ifPresent(predicates::add);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Builds an exact-match predicate on the {@code user} field, or returns empty if the value is
     * absent or blank.
     *
     * @param root the query root for {@link Document}
     * @param cb the criteria builder
     * @param user the username filter value, may be {@code null} or blank
     * @return an {@link Optional} containing the predicate, or empty if the filter is inactive
     */
    private static Optional<Predicate> userPredicate(
            Root<Document> root, CriteriaBuilder cb, String user) {
        if (user == null || user.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(cb.equal(root.get("user"), user));
    }

    /**
     * Builds a case-insensitive {@code LIKE %value%} predicate on the {@code fileName} field, or returns
     * empty if the value is absent or blank.
     *
     * @param root the query root for {@link Document}
     * @param cb the criteria builder
     * @param name the document fileName filter value, may be {@code null} or blank
     * @return an {@link Optional} containing the predicate, or empty if the filter is inactive
     */
    private static Optional<Predicate> namePredicate(
            Root<Document> root, CriteriaBuilder cb, String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(
                cb.like(cb.lower(root.get("fileName")), "%" + name.toLowerCase() + "%"));
    }

    /**
     * Builds an IN predicate on the {@code tags} collection using an inner join, or returns empty if
     * the tag list is absent or empty. Marks the query as {@code DISTINCT} to prevent duplicate rows
     * when multiple tags match.
     *
     * @param root the query root for {@link Document}
     * @param query the criteria query, used to set {@code DISTINCT}
     * @param tags the list of tag values to filter by, may be {@code null} or empty
     * @return an {@link Optional} containing the predicate, or empty if the filter is inactive
     */
    private static Optional<Predicate> tagsPredicate(
            Root<Document> root, CriteriaQuery<?> query, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Optional.empty();
        }
        Join<Document, String> tagsJoin = root.join("tags", JoinType.INNER);
        query.distinct(true);
        return Optional.of(tagsJoin.in(tags));
    }
}