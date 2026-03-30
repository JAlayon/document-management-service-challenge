package com.clara.ops.challenge.document_management_service_challenge.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.clara.ops.challenge.document_management_service_challenge.dto.in.DocumentSearchFilters;
import com.clara.ops.challenge.document_management_service_challenge.entity.Document;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DocumentSpecification} verifying that JPA {@link Predicate} objects are
 * assembled correctly for every combination of filter values.
 *
 * <p>All JPA Criteria API objects are replaced by Mockito mocks so no database is required. The
 * specification lambda is invoked directly with the mocked {@code (root, query, cb)} triple.
 *
 * <p>{@code @SuppressWarnings("unchecked")} is required to silence warnings that arise from
 * mocking raw or wildcard generic JPA types.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class DocumentSpecificationTest {

    /** JPA criteria root representing the {@link Document} entity in query expressions. */
    @Mock private Root<Document> root;

    /** JPA criteria query used to set the DISTINCT flag when filtering by tags. */
    @Mock private CriteriaQuery<?> query;

    /** JPA criteria builder used to construct individual predicates. */
    @Mock private CriteriaBuilder cb;

    /** Mock path returned by {@code root.get("user")}. */
    @Mock private Path<Object> userPath;

    /** Mock path returned by {@code root.get("fileName")}. */
    @Mock private Path<String> fileNamePath;

    /** Mock expression returned by {@code cb.lower(fileNamePath)}. */
    @Mock private Expression<String> lowerExpression;

    /** Mock join returned by {@code root.join("tags", INNER)}. */
    @Mock private Join<Document, String> tagsJoin;

    /** Mock predicate stub returned by {@code cb.equal(...)} calls. */
    @Mock private Predicate userPredicate;

    /** Mock predicate stub returned by {@code cb.like(...)} calls. */
    @Mock private Predicate fileNamePredicate;

    /** Mock predicate stub returned by {@code join.in(...)} calls. */
    @Mock private Predicate tagsPredicate;

    /** Mock predicate stub returned by {@code cb.and(...)} — the combined result. */
    @Mock private Predicate andPredicate;

    /**
     * Stubs {@code cb.and(Predicate[])} for every test. {@code lenient()} prevents strict-stub
     * failures in tests that focus on verifying that other predicates are <em>not</em> built.
     */
    @BeforeEach
    void stubCriteriaAndCombination() {
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(andPredicate);
    }

    /**
     * Verifies that when a non-blank user is supplied the specification builds an exact-match
     * equality predicate on the {@code user} column.
     */
    @Test
    void withFilters_shouldAddUserPredicate_whenUserIsProvided() {
        when(root.get("user")).thenReturn(userPath);
        when(cb.equal(userPath, "alice")).thenReturn(userPredicate);

        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters("alice", null, null));
        var result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).equal(userPath, "alice");
    }

    /**
     * Verifies that a {@code null} user value causes the user equality predicate to be omitted.
     */
    @Test
    void withFilters_shouldSkipUserPredicate_whenUserIsNull() {
        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters(null, null, null));
        spec.toPredicate(root, query, cb);

        verify(cb, never()).equal(any(), any());
    }

    /**
     * Verifies that a blank (whitespace-only) user value causes the user equality predicate to be
     * omitted.
     */
    @Test
    void withFilters_shouldSkipUserPredicate_whenUserIsBlank() {
        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters("   ", null, null));
        spec.toPredicate(root, query, cb);

        verify(cb, never()).equal(any(), any());
    }

    /**
     * Verifies that when a non-blank file name is supplied the specification builds a
     * case-insensitive {@code LIKE %value%} predicate on the {@code fileName} column.
     */
    @Test
    void withFilters_shouldAddFileNamePredicate_whenFileNameIsProvided() {
        doReturn(fileNamePath).when(root).get("fileName");
        when(cb.lower(any())).thenReturn(lowerExpression);
        when(cb.like(lowerExpression, "%report%")).thenReturn(fileNamePredicate);

        var spec =
                DocumentSpecification.withFilters(new DocumentSearchFilters(null, "Report", null));
        spec.toPredicate(root, query, cb);

        verify(cb).like(lowerExpression, "%report%");
    }

    /**
     * Verifies that a {@code null} file name causes the LIKE predicate to be omitted.
     */
    @Test
    void withFilters_shouldSkipFileNamePredicate_whenFileNameIsNull() {
        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters(null, null, null));
        spec.toPredicate(root, query, cb);

        verify(cb, never()).like(any(), any(String.class));
    }

    /**
     * Verifies that a blank (whitespace-only) file name causes the LIKE predicate to be omitted.
     */
    @Test
    void withFilters_shouldSkipFileNamePredicate_whenFileNameIsBlank() {
        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters(null, "   ", null));
        spec.toPredicate(root, query, cb);

        verify(cb, never()).like(any(), any(String.class));
    }

    /**
     * Verifies that when a non-empty tags list is supplied the specification joins the tags
     * collection, marks the query DISTINCT, and builds an IN predicate.
     */
    @Test
    void withFilters_shouldAddTagsPredicate_andSetDistinct_whenTagsAreProvided() {
        var tags = List.of("hr", "finance");
        doReturn(tagsJoin).when(root).join("tags", JoinType.INNER);
        when(tagsJoin.in(tags)).thenReturn(tagsPredicate);

        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters(null, null, tags));
        spec.toPredicate(root, query, cb);

        verify(root).join("tags", JoinType.INNER);
        verify(query).distinct(true);
        verify(tagsJoin).in(tags);
    }

    /**
     * Verifies that a {@code null} tags list causes the tags join and IN predicate to be skipped.
     */
    @Test
    void withFilters_shouldSkipTagsPredicate_whenTagsIsNull() {
        var spec = DocumentSpecification.withFilters(new DocumentSearchFilters(null, null, null));
        spec.toPredicate(root, query, cb);

        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    /**
     * Verifies that an empty tags list causes the tags join and IN predicate to be skipped.
     */
    @Test
    void withFilters_shouldSkipTagsPredicate_whenTagsIsEmpty() {
        var spec =
                DocumentSpecification.withFilters(new DocumentSearchFilters(null, null, List.of()));
        spec.toPredicate(root, query, cb);

        verify(root, never()).join(anyString(), any(JoinType.class));
    }

    /**
     * Verifies that when all three filters are populated the specification builds and combines all
     * three predicates in a single {@code AND} expression.
     */
    @Test
    void withFilters_shouldCombineAllPredicates_whenAllFiltersAreProvided() {
        var tags = List.of("hr");
        when(root.get("user")).thenReturn(userPath);
        when(cb.equal(userPath, "alice")).thenReturn(userPredicate);
        doReturn(fileNamePath).when(root).get("fileName");
        when(cb.lower(any())).thenReturn(lowerExpression);
        when(cb.like(lowerExpression, "%report%")).thenReturn(fileNamePredicate);
        doReturn(tagsJoin).when(root).join("tags", JoinType.INNER);
        when(tagsJoin.in(tags)).thenReturn(tagsPredicate);

        var spec =
                DocumentSpecification.withFilters(
                        new DocumentSearchFilters("alice", "Report", tags));
        var result = spec.toPredicate(root, query, cb);

        assertThat(result).isNotNull();
        verify(cb).equal(userPath, "alice");
        verify(cb).like(lowerExpression, "%report%");
        verify(tagsJoin).in(tags);
        verify(query).distinct(true);
        verify(cb).and(any(Predicate[].class));
    }
}
