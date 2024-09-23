#! /bin/bash

prepare_build(){
  ## 检查自定义的文件
  if [ -f ../bin/env.properties ];then
      cp -f ../bin/env.properties bin/env.properties
  fi
  if [ -f ../bin/setenv.sh ];then
      cp -f ../bin/setenv.sh bin/setenv.sh
  fi
  if [ -f ../bin/run.sh ];then
      cp -f ../bin/run.sh bin/run.sh
  fi
  if [ -f ../bin/install.sh ];then
      cp -f ../bin/install.sh bin/install.sh
  fi
  if [ -f ../tar.sh ];then
      cp -f ../tar.sh ./tar.sh
  fi
  if [ -f ../docker.sh ];then
      cp -f ../docker.sh ./docker.sh
  fi
  if [ -f ../deb.sh ];then
      cp -f ../deb.sh ./deb.sh
  fi
  find . -type f -name "*.sh" -exec chmod 744 {} \;
  find . -type f -name "*.sh" -exec dos2unix {} \;

  ## 引用setenv.sh，下面调用replace方法
  . "./bin/setenv.sh"

  ## 获取app_name和app_version（优先取env.properties，没有则从pom.xml获取），然后写到setenv.sh中
  line_app_name=$(< "./bin/env.properties" sed '/^#.*/d' | sed '/^[ \t ]*$/d' | grep = | sed 's/[ \t]*=[ \t]*/=/' | grep app_name)
  line_app_version=$(< "./bin/env.properties" sed '/^#.*/d' | sed '/^[ \t ]*$/d' | grep = | sed 's/[ \t]*=[ \t]*/=/' | grep app_version)
  line_app_home=$(< "./bin/env.properties" sed '/^#.*/d' | sed '/^[ \t ]*$/d' | grep = | sed 's/[ \t]*=[ \t]*/=/' | grep app_home)
  app_name=$(echo "$line_app_name" | awk -F '=' '{ key=$1; sub(/^[ \t]+/, "", key); sub(/[ \t]+$/, "", key); value=substr($0,length(key)+2); print value}')
  if [ -z "$app_name" ]; then
      app_name=$(grep -B 4 packaging ../pom.xml | grep artifactId | awk -F ">" '{print $2}' | awk -F "<" '{print $1}')
  fi
  app_version=$(echo "$line_app_version" | awk -F '=' '{ key=$1; sub(/^[ \t]+/, "", key); sub(/[ \t]+$/, "", key); value=substr($0,length(key)+2); print value}')
  if [ -z "$app_version" ]; then
      app_version=$(grep -B 4 packaging ../pom.xml | grep version | awk -F ">" '{print $2}' | awk -F "<" '{print $1}')
  fi
  app_home=$(echo "$line_app_home" | awk -F '=' '{ key=$1; sub(/^[ \t]+/, "", key); sub(/[ \t]+$/, "", key); value=substr($0,length(key)+2); print value}')
  if [ -z "$app_home" ]; then
      app_home="/opt/cowave/$app_name"
  fi

  ## 获取代码版本信息
  commit=$(git log -n 1 --pretty=oneline | awk '{print $1}')
  branch=$(git name-rev --name-only HEAD)
  codeVersion="$branch $commit"
  commit_msg=$(git log --pretty=format:"%s" -1)
  commit_time=$(git log --pretty=format:"%cd" -1)
  commit_author=$(git log --pretty=format:"%an" -1)
  echo "${app_name} ${app_version}(${branch} ${commit} @${commit_author})"

  ## 设置setenv.sh中的变量，比如app_name、app_version、app_home
  buildTime=$(date "+%Y-%m-%d %H:%M:%S")
  sed -i 's#export build_time=.*#export build_time="'"$buildTime"'"#' bin/setenv.sh
  sed -i 's#export app_name=.*#export app_name="'"$app_name"'"#' bin/setenv.sh
  sed -i 's#export app_version=.*#export app_version="'"$app_version"'"#' bin/setenv.sh
  sed -i 's#export app_home=.*#export app_home="'"$app_home"'"#' bin/setenv.sh
  sed -i 's#export code_version=.*#export code_version="'"$codeVersion"'"#' bin/setenv.sh

  ## 尝试将信息写入META-INF/info.yml（如果存在的话），打到jar里面
  if [ -f classes/META-INF/info.yml ];then
      ## info.application
      replace classes/META-INF/info.yml name "$app_name" 1
      replace classes/META-INF/info.yml version "$app_version" 1
      replace classes/META-INF/info.yml build "$buildTime" 1
      ## info.commit
      replace classes/META-INF/info.yml version \""$codeVersion"\" 2
      replace classes/META-INF/info.yml Msg \""$commit_msg"\" 1
      replace classes/META-INF/info.yml Time "$commit_time" 1
      replace classes/META-INF/info.yml Author "$commit_author" 1
      ## spring.application.name
      replace classes/META-INF/info.yml name "$app_name" 2
  fi

  ## 创建打包目录，拷贝: /bin、/config、changelog.md、install.sh
  app_source="$app_name"_"$app_version"
  mkdir -p "$app_source"/lib
  cp -rf bin "$app_source"
  if [ -f ../changelog.md ];then
      cp -f ../changelog.md "$app_source"
  else
      touch "$app_source"/changelog.md
  fi
  mv "$app_source"/bin/install.sh "$app_source"
  cp -rf classes/config "$app_source"
}

tar_build(){
  . "./bin/setenv.sh"
  . "./tar.sh"
  export app_source="$app_name"_"$app_version"
  build
}

docker_build(){
  . "./bin/setenv.sh"
  . "./docker.sh"
  export app_source="$app_name"_"$app_version"
  build
}

deb_build(){
  . "./bin/setenv.sh"
  . "./deb.sh"
  export app_source="$app_name"_"$app_version"
  build
}

case "$1" in
    prepare)
        prepare_build $2
        ;;
    tar)
        tar_build
        ;;
    docker)
        docker_build
        ;;
    deb)
        deb_build
        ;;
    *)
esac
