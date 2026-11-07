// src/utils/request.ts
import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosError,
  type AxiosResponse
} from 'axios';
import { ElMessage } from 'element-plus';

// 定义后端统一返回的数据格式接口
export interface ApiResponse<T = any> {
  code: number;
  msg?: string;
  data?: T;
}

// =========================================================================
// 1. 创建通用请求实例 (JSON 格式请求)
// =========================================================================
const request: AxiosInstance = axios.create({
  // 后端接口的基础地址，发请求时只需写 /api/xxx 即可
  baseURL: 'http://localhost:8080',
  // 超时时间：30秒，如果30秒后端没响应，自动断开
  timeout: 30000,
  // 允许跨域请求时携带 Cookie 等凭证（很重要）
  withCredentials: true,
  // 默认请求头：告诉后端我们发送的是 JSON 数据
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
});

// =========================================================================
// 2. 通用请求拦截器 (Request Interceptor) —— 出发前的安检站
// =========================================================================
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 🔥 核心步骤 A：从浏览器的 localStorage 中取出保存的 Token
    const token = localStorage.getItem('token');

    // 🔥 核心步骤 B：如果 Token 存在，把它塞进请求头，让后端识别你的身份
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    // 返回修改后的配置，请求才会真正发出去
    return config;
  },
  (error: AxiosError) => {
    // 如果请求在发送前就出错了（极少见），直接抛出错误
    return Promise.reject(error);
  }
);

// =========================================================================
// 3. 通用响应拦截器 (Response Interceptor) —— 回来后的质检员
// =========================================================================
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果后端返回了 2xx 的状态码（通常是200），说明请求成功
    // 我们直接把完整的响应对象返回给业务代码，业务代码自己去取 response.data
    return response;
  },
  (error: AxiosError) => {
    // 如果请求报错了（比如 404, 500, 401, 网络断开），交给统一的错误处理中心
    return handleError(error);
  }
);


// =========================================================================
// 4. 创建文件上传实例 (Multipart/form-data 格式请求)
// =========================================================================
// 单独建一个实例是因为文件上传的 Content-Type、超时时间与普通请求不同
const uploadRequest: AxiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 60000, // 文件上传可能较慢，把超时时间延长到60秒
  withCredentials: true,
});

// 文件上传的请求拦截器
uploadRequest.interceptors.request.use(config => {
  // 🔥 核心步骤 C：文件上传必须让浏览器自动设置 Content-Type 为 multipart/form-data
  // 所以要把默认的 json Content-Type 删掉，否则后端接收不到文件
  delete config.headers['Content-Type'];

  // 同样需要带上 Token 进行身份认证
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// 文件上传的响应拦截器
uploadRequest.interceptors.response.use(
  (response) => response, // 成功则原样返回
  (error: AxiosError) => handleError(error) // 失败则交给统一处理中心
);


// =========================================================================
// 5. 统一错误处理中心 (全局公用)
// =========================================================================
const handleError = (error: AxiosError) => {
  console.error('【请求或响应发生异常】', error);

  // 情况一：请求已经发出，并且后端返回了错误状态码（如 401, 403, 500）
  if (error.response) {
    const status = error.response.status;

    // 🔥 核心步骤 D：401 说明 Token 过期、无效或伪造
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录');
      // 清除失效的 Token 和用户信息
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      // 强制让浏览器跳转到登录页面
      window.location.href = '/login';
      // 抛出后端返回的错误信息，阻断后续操作
      return Promise.reject(error.response.data);
    }

    // 其他后端错误（如 500 服务器崩溃），返回后端的具体报错信息
    return Promise.reject(error.response.data || { code: -1, msg: '服务器内部错误' });
  }

  // 情况二：连后端都没连上（比如断网了，或者后端没启动）
  if (error.message === 'Network Error') {
    ElMessage.error('网络连接失败，请检查网络或后端服务');
    return Promise.reject({ code: -1, msg: '网络连接失败' });
  }

  // 情况三：请求超时（等了好久后端没响应）
  if (error.message.includes('timeout')) {
    ElMessage.error('请求超时，请稍后重试');
    return Promise.reject({ code: -1, msg: '请求超时' });
  }

  // 兜底：其他未知的错误
  return Promise.reject({ code: -1, msg: error.message || '请求失败' });
};

// 导出实例，供全项目使用
export default request;
export { request, uploadRequest };
