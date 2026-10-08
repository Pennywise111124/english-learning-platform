package com.example.englishlearningplatform.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.englishlearningplatform.dto.topic.TopicSort;
import com.example.englishlearningplatform.entity.ContentStatus;
import com.example.englishlearningplatform.entity.Level;
import com.example.englishlearningplatform.entity.Topic;
import com.example.englishlearningplatform.entity.UserProgress;

import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class TopicSpecifications {

    private TopicSpecifications() {
    }

    public static Specification<Topic> forLearners(String keyword, Level level, TopicSort sort) {
        return search(keyword, level, sort, ContentStatus.PUBLISHED);
    }

    public static Specification<Topic> forAdmin(String keyword, Level level, TopicSort sort, ContentStatus status) {
        return search(keyword, level, sort, status);
    }

    private static Specification<Topic> search(String keyword, Level level, TopicSort sort, ContentStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern, '\\');
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), pattern, '\\');
                predicates.add(cb.or(titleLike, descriptionLike));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (query != null && !Long.class.equals(query.getResultType())
                    && !long.class.equals(query.getResultType())) {
                List<Order> orders = new ArrayList<>();

                if (sort != null) {
                    switch (sort) {
                        case TITLE -> {
                            orders.add(cb.asc(cb.lower(root.get("title"))));
                            orders.add(cb.asc(root.get("id")));
                        }
                        case POPULAR -> {
                            Subquery<Long> subquery = query.subquery(Long.class);
                            Root<UserProgress> up = subquery.from(UserProgress.class);
                            subquery.select(cb.count(up)).where(cb.equal(up.get("topic"), root));

                            orders.add(cb.desc(subquery));
                            orders.add(cb.desc(root.get("id")));
                        }
                        case NEWEST -> {

                        }
                    }
                }

                if (orders.isEmpty()) {
                    orders.add(cb.desc(root.get("createdAt")));
                    orders.add(cb.desc(root.get("id")));
                }

                query.orderBy(orders);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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