package hexlet.code.controllers;


import hexlet.code.model.pages.BasePage;
import io.javalin.http.Context;

import java.util.Collections;

public final class RootController {

    public void welcome(Context ctx) {
        var page = new BasePage();
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("index.jte", Collections.singletonMap("page", page));
    }
}

