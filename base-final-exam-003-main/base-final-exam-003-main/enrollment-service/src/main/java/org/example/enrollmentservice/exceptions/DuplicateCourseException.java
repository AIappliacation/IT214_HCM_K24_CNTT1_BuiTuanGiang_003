package org.example.enrollmentservice.exceptions;

public class DuplicateCourseException extends RuntimeException {

    public DuplicateCourseException(Long courseId) {
        super("Mỗi khóa học chỉ được xuất hiện một lần trong phiếu đăng ký");
    }
}
