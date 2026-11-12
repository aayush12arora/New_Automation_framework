package tests;

import framework.api.torsavo.TorsavoApi;
import framework.api.torsavo.TorsavoApi.ProductInfo;
import framework.base.TorsavoBaseTest;
import framework.pages.ProductPage;
import framework.pages.components.HeaderComponent;
import org.testng.annotations.Test;

import java.util.List;

import static framework.constants.TestGroups.REGRESSION;
import static framework.constants.TestGroups.SMOKE;

/** Header search with live suggestions (debounced API search). */
public class HeaderSearchTest extends TorsavoBaseTest {

    private static final String NONSENSE = "zzqx-no-such-piece";

    /** A search term guaranteed to match: the first word of a real product name. */
    private String knownTerm(ProductInfo product) {
        return product.name().split("\\s+")[0];
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "Typing a product name shows live suggestions")
    public void searchShowsLiveSuggestions() {
        ProductInfo product = TorsavoApi.inStockProduct();
        HeaderComponent header = new HeaderComponent();
        header.search(product.name());
        assertions.assertTrue(header.areSuggestionsShown(), "Suggestions dropdown should appear");
        List<String> suggestions = header.suggestionTexts();
        assertions.assertTrue(suggestions.stream().anyMatch(s -> s.toLowerCase().contains(product.name().toLowerCase())),
                "Suggestions should include '" + product.name() + "' — got " + suggestions);
    }

    @Test(groups = {REGRESSION}, description = "Choosing a suggestion opens that product's page")
    public void selectingSuggestionOpensProductPage() {
        ProductInfo product = TorsavoApi.inStockProduct();
        HeaderComponent header = new HeaderComponent();
        header.search(product.name());
        header.openFirstSuggestion();
        waitForUrl("/product/");
        assertions.assertTrue(!new ProductPage().waitForLoad().name().isBlank(), "Product page should show a product name");
    }

    @Test(groups = {REGRESSION}, description = "Pressing Enter opens the shop with the search query in the URL")
    public void pressingEnterOpensShopWithQuery() {
        String term = knownTerm(TorsavoApi.inStockProduct());
        HeaderComponent header = new HeaderComponent();
        header.search(term);
        header.submitSearch();
        waitForUrl("/products?search=");
        assertions.assertContains(getDriver().getCurrentUrl(), "search=" + term, "URL should carry the search term");
    }

    /*
     * Note: SearchDropdown has a "No pieces found / Browse All Pieces" panel, but
     * useProductSearch closes the dropdown whenever a search returns nothing, so that panel
     * never renders. These tests check what shoppers actually get today.
     */

    @Test(groups = {REGRESSION}, description = "A term with no matches shows no suggestions")
    public void unknownTermShowsNoSuggestions() {
        HeaderComponent header = new HeaderComponent();
        header.search(NONSENSE);
        assertions.assertTrue(!header.areSuggestionsShown(3), "No product suggestions should be shown for '" + NONSENSE + "'");
    }

    @Test(groups = {REGRESSION}, description = "Search matches regardless of letter case")
    public void searchIsCaseInsensitive() {
        ProductInfo product = TorsavoApi.inStockProduct();
        HeaderComponent header = new HeaderComponent();
        header.search(product.name().toUpperCase());
        assertions.assertTrue(header.areSuggestionsShown(), "Upper-case search should still find the product");
        assertions.assertTrue(header.suggestionTexts().stream().anyMatch(s -> s.equalsIgnoreCase(product.name())
                        || s.toLowerCase().contains(product.name().toLowerCase())),
                "Suggestions should include '" + product.name() + "'");
    }
}
