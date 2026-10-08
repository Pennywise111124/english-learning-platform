package com.example.englishlearningplatform.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.englishlearningplatform.dto.dictation.DictationProgress;
import com.example.englishlearningplatform.dto.dictation.DictationSort;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.DictationLesson;
import com.example.englishlearningplatform.entity.DictationResult;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class DictationLessonSpecifications {

    private DictationLessonSpecifications() {
    }

    public static Specification<DictationLesson> catalog(Long userId, String keyword, Long topicId,
            Level level, DictationProgress progress, DictationSort sort) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<DictationLesson, Topic> topic = root.join("topic");

            predicates.add(cb.isNotNull(root.get("mediaUrl")));
            predicates.add(cb.equal(root.get("status"), ContentStatus.PUBLISHED));
            predicates.add(cb.equal(topic.get("status"), ContentStatus.PUBLISHED));

            if (topicId != null) {
                predicates.add(cb.equal(root.get("topic").get("id"), topicId));
            }

            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(topic.get("title")), pattern, '\\')));
            }

            if (progress == DictationProgress.NEW) {
                predicates.add(cb.not(cb.exists(userResults(query, cb, root, userId))));
            } else if (progress == DictationProgress.PRACTICED) {
                predicates.add(cb.exists(userResults(query, cb, root, userId)));
            }

            if (query != null && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                List<Order> orders = new ArrayList<>();
                DictationSort effective = sort == null ? DictationSort.NEWEST : sort;

                switch (effective) {
                    case TITLE -> {
                        orders.add(cb.asc(cb.lower(root.get("title"))));
                        orders.add(cb.asc(root.get("id")));
                    }
                    case NEWEST -> orders.add(cb.desc(root.get("id")));
                    case RECENT -> {
                        Expression<Integer> practicedFirst = cb.<Integer>selectCase()
                                .when(cb.exists(userResults(query, cb, root, userId)), 0)
                                .otherwise(1);
                        orders.add(cb.asc(practicedFirst));
                        orders.add(cb.desc(lastAttempt(query, cb, root, userId)));
                        orders.add(cb.desc(root.get("id")));
                    }
                }
                query.orderBy(orders);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Subquery<Long> userResults(CriteriaQuery<?> query, CriteriaBuilder cb,
            Root<DictationLesson> root, Long userId) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<DictationResult> r = sub.from(DictationResult.class);
        sub.select(r.<Long>get("id"))
                .where(cb.equal(r.get("lesson"), root),
                        cb.equal(r.get("user").get("id"), userId));
        return sub;
    }

    private static Subquery<Instant> lastAttempt(CriteriaQuery<?> query, CriteriaBuilder cb,
            Root<DictationLesson> root, Long userId) {
        Subquery<Instant> sub = query.subquery(Instant.class);
        Root<DictationResult> r = sub.from(DictationResult.class);
        sub.select(cb.greatest(r.<Instant>get("createdAt")))
                .where(cb.equal(r.get("lesson"), root),
                        cb.equal(r.get("user").get("id"), userId));
        return sub;
    }

    private static String escapeLike(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}