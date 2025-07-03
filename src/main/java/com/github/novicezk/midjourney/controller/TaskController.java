package com.github.novicezk.midjourney.controller;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.github.novicezk.midjourney.dto.TaskConditionDTO;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.loadbalancer.DiscordLoadBalancer;
import com.github.novicezk.midjourney.service.TaskStoreService;
import com.github.novicezk.midjourney.support.Task;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Api(tags = "任务查询")
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {
	private final TaskStoreService taskStoreService;
	private final DiscordLoadBalancer discordLoadBalancer;

	@ApiOperation(value = "指定ID获取任务")
	@GetMapping("/{id}/fetch")
	public Task fetch(@ApiParam(value = "任务ID") @PathVariable String id) {
		Optional<Task> queueTaskOptional = this.discordLoadBalancer.getQueueTasks().stream()
				.filter(t -> CharSequenceUtil.equals(t.getId(), id)).findFirst();
		return queueTaskOptional.orElseGet(() -> {
			Task task = this.taskStoreService.get(id);
			if(null != task && task.getAction() == TaskAction.IMAGINE && task.getStatus() == TaskStatus.SUCCESS) {
				String imageUrl = task.getImageUrl();
				if(StrUtil.isNotBlank(imageUrl)) {
					String uploadUrl = "https://api.yingyunai.com/yingyun-boot/app/origin/upload";
                    InputStream inputStream = null;
                    try {
                        inputStream = new URL(imageUrl).openStream();
						byte[] fileBytes = inputStream.readAllBytes(); // 读取整个文件内容
						ByteArrayBody byteArrayBody = new ByteArrayBody(fileBytes, ContentType.DEFAULT_BINARY, "mj-image.png");
						MultipartEntityBuilder builder = MultipartEntityBuilder.create();
						builder.addPart("file", byteArrayBody); // 字段名为 file
						HttpEntity entity = builder.build();

						CloseableHttpClient client = HttpClients.createDefault();
						HttpPost req = new HttpPost(uploadUrl);
						req.setEntity(entity);
						CloseableHttpResponse response = client.execute(req);
						String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
						int statusCode = response.getStatusLine().getStatusCode();
						if (statusCode == 200) {
							JSONObject jsonObject = new JSONObject(responseString);
							String newUrl = jsonObject.getStr("result");
							if(StrUtil.isNotBlank(newUrl)) {
								task.setImageUrl(newUrl);
							}
						}

					} catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    finally {
						if(null != inputStream) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
					}
				}
			}
			return task;
		});
	}

	@ApiOperation(value = "查询任务队列")
	@GetMapping("/queue")
	public List<Task> queue() {
		return this.discordLoadBalancer.getQueueTasks().stream()
				.sorted(Comparator.comparing(Task::getSubmitTime))
				.toList();
	}

	@ApiOperation(value = "查询所有任务")
	@GetMapping("/list")
	public List<Task> list() {
		return this.taskStoreService.list().stream()
				.sorted((t1, t2) -> CompareUtil.compare(t2.getSubmitTime(), t1.getSubmitTime()))
				.toList();
	}

	@ApiOperation(value = "根据ID列表查询任务")
	@PostMapping("/list-by-condition")
	public List<Task> listByIds(@RequestBody TaskConditionDTO conditionDTO) {
		if (conditionDTO.getIds() == null) {
			return Collections.emptyList();
		}
		List<Task> result = new ArrayList<>();
		Set<String> notInQueueIds = new HashSet<>(conditionDTO.getIds());
		this.discordLoadBalancer.getQueueTasks().forEach(t -> {
			if (conditionDTO.getIds().contains(t.getId())) {
				result.add(t);
				notInQueueIds.remove(t.getId());
			}
		});
		notInQueueIds.forEach(id -> {
			Task task = this.taskStoreService.get(id);
			if (task != null) {
				result.add(task);
			}
		});
		return result;
	}

}
