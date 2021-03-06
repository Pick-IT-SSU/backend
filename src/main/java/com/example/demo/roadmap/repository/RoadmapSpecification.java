package com.example.demo.roadmap.repository;

import com.example.demo.roadmap.RoadMapGroup;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class RoadmapSpecification {
    public static Specification<RoadMapGroup> getExistingRoadmap(){
        return ((root,query,criteriaBuilder)-> criteriaBuilder.equal(root.get("roadmapGroupStatus"),1));
    }

    public static Specification<RoadMapGroup> getRoadmapByFilter(String[] keywords) {
        return ((root, query, criteriaBuilder) -> {
            Predicate existence=criteriaBuilder.equal(root.get("roadmapGroupStatus"),1);
            List<Predicate> predicates=new ArrayList<>();
            for(String keyword:keywords){
                predicates.add(criteriaBuilder.like(root.get("roadmapGroupRecommendation").as(String.class),"%"+keyword+"%"));
                predicates.add(criteriaBuilder.like(root.get("roadmapGroupTitle").as(String.class),"%"+keyword+"%"));
            }
            Predicate or = criteriaBuilder.or(predicates.toArray(new Predicate[predicates.size()]));
            return criteriaBuilder.and(existence,or);

        });


    }
}
