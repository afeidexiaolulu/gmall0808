package com.atguigu.gmall0808.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	private JestClient jestClient;
	@Test
	public void contextLoads() {
	}

	@Test
	public void testES() throws IOException {
		// 自定义dsl 语句
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"match\": {\n" +
				"      \"actorList.name\":\"张译\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		// 准备执行dsl 语句
		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();
		SearchResult searchResult = jestClient.execute(search);

		// 循环出执行后的结果集
		List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
		for (SearchResult.Hit<Map, Void> hit : hits) {
			Map map = hit.source;
			System.out.println(map.get("name")); // 红海行动
		}
	}

}

