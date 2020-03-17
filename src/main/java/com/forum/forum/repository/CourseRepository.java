package com.forum.forum.repository;

import com.forum.forum.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Course findByName(String courseName);
}
