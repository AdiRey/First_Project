package pl.javadev.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pl.javadev.exception.other.ConflictIdException;
import pl.javadev.exception.other.InvalidIdException;
import pl.javadev.exception.other.WrongTimeException;
import pl.javadev.lesson.*;
import pl.javadev.teacher.Teacher;
import pl.javadev.teacher.TeacherDto;
import pl.javadev.teacher.TeacherRepository;
import pl.javadev.user.User;
import pl.javadev.user.UserRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class LessonServiceImpl implements LessonService{
    private LessonRepository lessonRepository;
    private UserRepository userRepository;
    private TeacherRepository teacherRepository;

    public LessonServiceImpl(LessonRepository lessonRepository, UserRepository userRepository, TeacherRepository teacherRepository) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
    }

    public Page<LessonDto> findAllLessonsUsingPaging(int numberOfPage, String sortText, String text) {
        Sort sort;
        if (sortText.equals("DESC"))
            sort = Sort.by(new Sort.Order(Sort.Direction.DESC, "title"));
        else
            sort = Sort.by(new Sort.Order(Sort.Direction.ASC, "title"));
        return lessonRepository.findAllByTitleContainingIgnoreCase
                (text, PageRequest.of(numberOfPage, 20, sort)).map(LessonMapper::map);
    }

    @Transactional
    public LessonDto addUsers(Long id, Long userId) {
        try {
            Optional<Lesson> foundLesson = lessonRepository.findById(id);
            Lesson lesson = foundLesson.get();
            if (userId == null)
                throw new InvalidIdException();
            Optional<User> foundUser = userRepository.findById(userId);
            User user = foundUser.get();
            lesson.addUser(user);
            user.addLesson(lesson);
            return LessonMapper.map(lesson);
        } catch (NoSuchElementException e) {
            throw new InvalidIdException();
        }
    }

    @Transactional
    public LessonDto addTeacher(Long id, TeacherDto teacherDto) {
        try {
            Optional<Lesson> foundLesson = lessonRepository.findById(id);
            Lesson lesson = foundLesson.get();
            Optional<Teacher> foundTeacher = teacherRepository.findById(teacherDto.getId());
            lesson.setTeacher(foundTeacher.get());
            return LessonMapper.map(lesson);
        } catch (NoSuchElementException e) {
            throw new InvalidIdException();
        }
    }

    public LessonDto findLesson(Long id) {
        try {
            Optional<Lesson> foundLesson = lessonRepository.findById(id);
            return LessonMapper.map(foundLesson.get());
        } catch (NoSuchElementException e) {
            throw new InvalidIdException();
        }
    }

    public LessonDto save(LessonRegistrationDto dto) {
        Lesson savedLesson = lessonRepository.save(LessonRegistrationMapper.map(dto));
        return LessonMapper.map(savedLesson);
    }

    public LessonDto findById(Long id) {
        Optional<Lesson> lesson = lessonRepository.findById(id);
        return lesson.map(LessonMapper::map).orElse(null);
    }

    @Transactional
    public LessonDto editLesson(Long id, LessonRegistrationDto dto) {
        try {
            if (!id.equals(dto.getId()))
                throw new ConflictIdException();
            Optional<Lesson> foundOne = lessonRepository.findById(dto.getId());
            Lesson lesson = foundOne.get();
            if (!lesson.getStart().isBefore(LocalDateTime.now())) {
                lesson.setTitle(dto.getTitle());
                lesson.setDescription(dto.getDescription());
                lesson.setStart(dto.getStart());
                lesson.setEnd(dto.getEnd());
            }
            return LessonMapper.map(lesson);
        } catch (NoSuchElementException e) {
            throw new InvalidIdException();
        }
    }

    public LessonDto delete(Long id) {
        try {
            Optional<Lesson> foundLesson = lessonRepository.findById(id);
            Lesson lesson = foundLesson.get();
            if (lesson.getStart().isAfter(LocalDateTime.now())) {
                LessonDto deletedOne = LessonMapper.map(lesson);
                lessonRepository.delete(lesson);
                return deletedOne;
            } else {
                throw new WrongTimeException();
            }
        } catch (NoSuchElementException e) {
            throw new InvalidIdException();
        }
    }

    public List<LessonDto> deleteAll() {
        List<LessonDto> lessons = new LinkedList<>();
        for (Lesson lesson : lessonRepository.findAll()) {
            if (lesson.getStart().isAfter(LocalDateTime.now())) {
                lessons.add(LessonMapper.map(lesson));
                lessonRepository.delete(lesson);
            }
        }
        return lessons;
    }
}