package com.atguigu.gmall0808.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class FileUploadController {

    @Value("${fileServer.url}") // 表示从配置文件中取得数据 ，当前类，必须在spring 容器中！
    private String fileUrl;  // fileUrl = http://192.168.67.211

    /*
        <from action = "" ,method="",entype="">
            <input type="file" name="file">
            <input type="submit" value="上传">

        </form>
     */
    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile file) throws IOException, MyException {
        String imgUrl=fileUrl;
        if (file!=null){
            // 上传之后要返回的图片路径
            // 将图片上传到图片服务器，上传成功之后，并返回图片地址
            // 读取配置文件
            String configFile  = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getConnection();
            StorageClient storageClient = new StorageClient(trackerServer, null);
//            String orginalFilename = "e://img//zly.jpg";
            // 上传文件名称
            String originalFilename = file.getOriginalFilename();
            // 取得文件名称的后缀名
            String extName  = StringUtils.substringAfterLast(originalFilename, ".");

            String[] upload_file = storageClient.upload_file(file.getBytes(), extName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];
                // imgUrl=http://192.168.67.211
                imgUrl+="/"+path;

//                System.out.println("s = " + s);
        //			s = group1
        //			s = M00/00/00/wKhD01wXzCuAAGKfAACGx2c4tJ4054.jpg
            }
        }

        // 变成实现软编码：将重要可变的信息放入配置文件中 application.properties
        // http://192.168.67.211/group1/M00/00/00/wKhD01wXzCuAAGKfAACGx2c4tJ4054.jpg
//        return "https://m.360buyimg.com/babel/jfs/t5137/20/1794970752/352145/d56e4e94/591417dcN4fe5ef33.jpg";
        return imgUrl;
    }
}
