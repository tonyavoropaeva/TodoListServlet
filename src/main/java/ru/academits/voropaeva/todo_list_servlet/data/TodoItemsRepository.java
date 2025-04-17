package ru.academits.voropaeva.todo_list_servlet.data;

import java.util.List;

public interface TodoItemsRepository {
    List<TodoItem> getAll();

    void create(TodoItem item);

    void update(TodoItem item);

    void delete(int itemId);
}