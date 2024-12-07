package hexlet.code.model.pages;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class UrlListPage extends BasePage {
    private List<Url> urls;
    private Map<Long, UrlCheck> latestChecks;
}
