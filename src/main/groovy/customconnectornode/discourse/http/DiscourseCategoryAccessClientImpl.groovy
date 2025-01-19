/*
 * Copyright (c) 2024 Exalate (https://exalate.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package customconnectornode.discourse.http


import customconnectornode.discourse.api.DiscourseCategoryAccessClient
import customconnectornode.discourse.api.DiscourseClient
import customconnectornode.discourse.domain.DiscourseCategory

/**
 * Implementation of the DiscourseCategoryAccessClient that uses the DiscourseClient to retrieve categories.
 */
class DiscourseCategoryAccessClientImpl implements DiscourseCategoryAccessClient {

    private final DiscourseClient discourseClient
    private final List<DiscourseCategory> categories = []


    public DiscourseCategoryAccessClientImpl(DiscourseClient discourseClient) {
        this.discourseClient = discourseClient;
    }


    private void buildCategories() {
        if (categories.size() > 0) {
            return
        }


        String path = "/categories.json"
        Map<String, List<String>> params = new HashMap<>()
        params.put("include_subcategories", ["false"])

        Map result = discourseClient.get(path, params)
        Map categoryList = result.category_list as Map

        categories.clear()
        categoryList.categories.each { Map categoryData ->
            categories.add(DiscourseCategory.fromJson(categoryData))
        }
    }

    @Override
    public List<DiscourseCategory> fetchAllCategories() {
        buildCategories()
        return categories
    }

    @Override
    DiscourseCategory fetchCategoryById(Integer categoryId) {
        buildCategories()
        return categories.find { it.id == categoryId }
    }

    @Override
    DiscourseCategory fetchCategoryByName(String categoryName) {
        buildCategories()
        return categories.find { it.name == categoryName }
    }
}
