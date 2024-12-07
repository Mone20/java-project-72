@import hexlet.code.model.web.Routes
@import java.time.format.DateTimeFormatter
@param hexlet.code.model.pages.UrlListPage page

!{var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");}

@template.header(
page = page,
content = @`
    <div class="container-lg mt-5">
        <h1>Сайты</h1>

        <table class="table table-bordered table-hover mt-3">
            <thead>
            <tr>
                <th class="col-1">ID</th>
                <th>Имя</th>
                <th class="col-2">Последняя проверка</th>
                <th class="col-1">Код ответа</th>
            </tr>
            </thead>
            <tbody>
            @for(var url : page.getUrls())
                !{var urlCheck = page.getLatestChecks().get(url.getId());}

                <tr>
                    <td>
                        ${url.getId()}
                    </td>
                    <td>
                        <a href="${Routes.urlPath(url.getId())}">${url.getName()}</a>
                    </td>
                    <td>
                        ${urlCheck == null ? "" : urlCheck.getCreatedAt().toLocalDateTime().format(formatter)}
                    </td>
                    <td>
                        ${urlCheck == null ? "" : String.valueOf(urlCheck.getStatusCode())}
                    </td>

                </tr>
            @endfor
            </tbody>
        </table>
    </div>
`
))