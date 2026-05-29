package com.nudgeup.app.ui.main;

import com.nudgeup.app.data.dto.response.TaskResponse;
import com.nudgeup.app.domain.User;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class MainViewModelSortTest {

    private static final User USER_가 = new User(1, "가나다");
    private static final User USER_나 = new User(2, "나다라");

    private List<TaskResponse> makeTasks() {
        return Arrays.asList(
            new TaskResponse(1, "Task1", "PENDING",     "2026-06-10", USER_나),
            new TaskResponse(2, "Task2", "IN_PROGRESS", "2026-06-01", USER_가),
            new TaskResponse(3, "Task3", "COMPLETED",   null,         USER_가)
        );
    }

    @Test
    public void sortByDeadline_nullLast() {
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(
            makeTasks(), MainViewModel.SortMode.BY_DEADLINE);
        assertEquals("2026-06-01", sorted.get(0).getDueDate());
        assertEquals("2026-06-10", sorted.get(1).getDueDate());
        assertNull(sorted.get(2).getDueDate());
    }

    @Test
    public void sortByPerson_alphabetical() {
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(
            makeTasks(), MainViewModel.SortMode.BY_PERSON);
        assertEquals("가나다", sorted.get(0).getAssignee().getName());
        assertEquals("가나다", sorted.get(1).getAssignee().getName());
        assertEquals("나다라", sorted.get(2).getAssignee().getName());
    }

    @Test
    public void sortDefault_completedLast() {
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(
            makeTasks(), MainViewModel.SortMode.DEFAULT);
        assertEquals("COMPLETED", sorted.get(sorted.size() - 1).getStatus());
    }

    @Test
    public void sortByPerson_nullAssigneeLast() {
        List<TaskResponse> tasks = Arrays.asList(
            new TaskResponse(1, "A", "PENDING", "2026-06-01", null),
            new TaskResponse(2, "B", "PENDING", "2026-06-01", USER_가)
        );
        List<TaskResponse> sorted = MainViewModel.sortTasksStatic(tasks, MainViewModel.SortMode.BY_PERSON);
        assertEquals(USER_가.getName(), sorted.get(0).getAssignee().getName());
        assertNull(sorted.get(1).getAssignee());
    }
}
