package hexlet.code.controllers;

import hexlet.code.model.UrlCheck;
import hexlet.code.model.pages.UrlListPage;
import hexlet.code.model.pages.UrlPage;
import hexlet.code.model.web.Routes;
import hexlet.code.repo.UrlCheckRepository;
import hexlet.code.repo.UrlRepository;
import io.javalin.http.Context;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import hexlet.code.model.Url;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

import static io.javalin.rendering.template.TemplateUtil.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class UrlsController {

    private final UrlRepository urlRepository;
    private final UrlCheckRepository urlCheckRepository;

    public void listUrls(Context ctx) throws SQLException {
        List<Url> urls = urlRepository.getEntities();
        Map<Long, UrlCheck> urlChecks = urlCheckRepository.findLatestChecks();
        var page = new UrlListPage(urls, urlChecks);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("urls/index.jte", model("page", page));
    }



    public void createUrl(Context ctx) throws SQLException {
        var inputUrl = ctx.formParam("url");
        URL parsedUrl;
        try {
            var uri = new URI(inputUrl);
            parsedUrl = uri.toURL();
        } catch (Exception e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect(Routes.rootPath());
            return;
        }

        String normalizedUrl = String
                .format(
                        "%s://%s%s",
                        parsedUrl.getProtocol(),
                        parsedUrl.getHost(),
                        parsedUrl.getPort() == -1 ? "" : ":" + parsedUrl.getPort()
                )
                .toLowerCase();

        Url url = urlRepository.findByName(normalizedUrl).orElse(null);

        if (url != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "info");
        } else {
            Url newUrl = new Url(normalizedUrl);
            urlRepository.save(newUrl);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
        }

        ctx.redirect("/urls", HttpStatus.forStatus(302));
    }



    public void showUrl(Context ctx) throws SQLException {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        var url = urlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Url with id = " + id + " not found"));

        var urlChecks = urlCheckRepository.findByUrlId(id);

        var page = new UrlPage(url, urlChecks);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));

        ctx.render("urls/show.jte", model("page", page));
    }



}
