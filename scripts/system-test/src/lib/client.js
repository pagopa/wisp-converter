const axios = require("axios");

async function call(method, url, body, headers) {
  if (method === 'GET') {
    return await get(url, headers)
  }
  if (method === 'POST') {
    return await post(url, body, headers)
  }
  if (method === 'PUT') {
    return await put(url, body, headers)
  }
  if (method === 'DELETE') {
    return await del(url, headers)
  }
}

async function get(url, headers) {
  return await axios.get(url, headers)
      .then(res => {
        return res;
      })
      .catch(error => {
        return error.response;
      });
}

async function post(url, body, headers) {
  return await axios.post(url, body, headers)
      .then(res => {
        return res;
      })
      .catch(error => {
        return error.response;
      });
}

async function put(url, body, headers) {
  return await axios.put(url, body, headers)
      .then(res => {
        return res;
      })
      .catch(error => {
        return error.response;
      });
}

async function del(url, headers) {
  return await axios.delete(url, headers)
      .then(res => {
        return res;
      })
      .catch(error => {
        return error.response;
      });
}

module.exports = {call}