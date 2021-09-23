# insightface_ncnn_andorid_example

## 公司有个需求 是通过人脸识别APP相册里所有同一个人，初步考虑使用insightface  +   ncnn的方式


### 参考 https://github.com/MirrorYuChen/ncnn_example  项目导入制作了安卓版本


### 使用步骤 下载https://github.com/MirrorYuChen/ncnn_example 中的models  复制到 sdcard 新建目录mtcnn 下，

### 运行项目 选择图片进行人脸识别  和相似度对比  


###  人脸比对流程比较简单   DetectFace  = 》 ExtractKeypoints -》AlignFace-》CalculateSimilarity。即人脸发现，抽取特征点，人脸对齐，和特征值相似度计算，值越接近1 越可以判断是同一个人
