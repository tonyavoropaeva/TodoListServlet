package ru.academits.voropaeva.todo_list_servlet.servlets;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.text.StringEscapeUtils;
import ru.academits.voropaeva.todo_list_servlet.data.TodoItem;
import ru.academits.voropaeva.todo_list_servlet.data.TodoItemsInMemoryRepository;
import ru.academits.voropaeva.todo_list_servlet.data.TodoItemsRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("")
public class TodoListServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");

        PrintWriter printWriter = resp.getWriter();

        String baseUrl = getServletContext().getContextPath() + "/";

        HttpSession session = req.getSession(false);
        String createErrorHtml = "";

        if (session != null) {
            String createError = (String) session.getAttribute("createError");

            if (createError != null) {
                createErrorHtml = "<div>%s</div>".formatted(StringEscapeUtils.escapeHtml4(createError));
            }

            session.removeAttribute("createError");
        }

        TodoItemsRepository todoItemsRepository = new TodoItemsInMemoryRepository();
        List<TodoItem> todoItems = todoItemsRepository.getAll();

        StringBuilder todoListHtml = new StringBuilder();

        for (TodoItem todoItem : todoItems) {
            String updateErrorHtml = "";

            if (session != null) {
                String updateError = (String) session.getAttribute("updateError_" + todoItem.getId());

                if (updateError != null) {
                    updateErrorHtml = "<div>%s</div>".formatted(StringEscapeUtils.escapeHtml4(updateError));
                    session.removeAttribute("updateError_" + todoItem.getId());
                }
            }

            todoListHtml
                    .append("""
                            <li>
                                <form action="%s" method="POST">
                                    <input type="text" name="text" value="%s">
                                    <button name="action" value="update" type="submit">Save</button>
                                    <button name="action" value="delete" type="submit">Delete</button>
                                    <input type="hidden" name="id" value="%s">
                                    %s
                                </form>
                            </li>
                            """.formatted(baseUrl, StringEscapeUtils.escapeHtml4(todoItem.getText()), todoItem.getId(), updateErrorHtml))
                    .append("\n");
        }

        printWriter.println("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>TODO List Servlets</title>
                    <meta charset="UTF-8">
                </head>
                <body>
                    <h1>TODO List Servlets</h1>
                    
                    <form action="%s" method="POST">
                        <input name="text" type="text">
                        <button name="action" value="create" type="submit">Create</button>
                        %s
                    </form>
                    <ul>
                        %s
                    </ul>
                </body>
                </html>
                """.formatted(baseUrl, createErrorHtml, todoListHtml));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter("action");

        if (action == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing action parameter");

            return;
        }

        switch (action) {
            case "create" -> {
                String text = req.getParameter("text").trim();

                if (text.isEmpty()) {
                    HttpSession session = req.getSession();
                    session.setAttribute("createError", "Text must be not empty");
                } else {
                    TodoItemsRepository todoItemsRepository = new TodoItemsInMemoryRepository();
                    todoItemsRepository.create(new TodoItem(text));
                }
            }
            case "update" -> {
                try {
                    int id = validateId(req, resp);

                    String text = req.getParameter("text").trim();

                    if (text.isEmpty()) {
                        HttpSession session = req.getSession();
                        session.setAttribute("updateError_" + id, "Text must be not empty");
                    } else {
                        TodoItemsRepository todoItemsRepository = new TodoItemsInMemoryRepository();
                        todoItemsRepository.update(new TodoItem(id, text));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            case "delete" -> {
                try {
                    int id = validateId(req, resp);
                    TodoItemsRepository todoItemsRepository = new TodoItemsInMemoryRepository();
                    todoItemsRepository.delete(id);
                } catch (IllegalArgumentException ignored) {
                }
            }
            default -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action parameter");
        }

        resp.sendRedirect(getServletContext().getContextPath() + "/");
    }

    private int validateId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idString = req.getParameter("id");

        if (idString == null) {
            String errorMessage = "Missing id parameter";

            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            return Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            String errorMessage = "Invalid id parameter";

            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage);
            throw new NumberFormatException(errorMessage);
        }
    }
}