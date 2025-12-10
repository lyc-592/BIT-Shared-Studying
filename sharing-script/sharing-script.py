import csv
import chardet
import os


def detect_encoding(file_path):
    """检测文件编码"""
    with open(file_path, 'rb') as f:
        rawdata = f.read(10000)  # 读取文件前10000字节来检测编码
        result = chardet.detect(rawdata)
        return result['encoding']


def process_courses(csv_file, all_majors=None):
    """处理课程CSV文件，提取课程信息和专业-课程关系"""

    if all_majors is None:
        all_majors = set()

    courses = {}  # 课程号 -> 课程名
    major_course_pairs = []  # 专业名 -> 课程号

    # 先检测文件编码
    try:
        encoding = detect_encoding(csv_file)
        print(f"处理文件 {csv_file}: 检测到文件编码: {encoding}")
    except:
        encoding = 'utf-8'
        print(f"处理文件 {csv_file}: 无法检测编码，使用默认编码: {encoding}")

    # 尝试不同的编码方式
    encodings_to_try = [encoding, 'gbk', 'gb2312', 'utf-8-sig', 'latin1', 'cp1252']

    for enc in encodings_to_try:
        try:
            with open(csv_file, 'r', encoding=enc) as f:
                # 尝试读取CSV文件
                # 先读取一行看看列名
                sample = f.readline()
                print(f"处理文件 {csv_file}: 使用编码 {enc} 读取成功，第一行样本: {sample[:100]}...")
                f.seek(0)  # 回到文件开头

                # 使用csv.reader读取
                reader = csv.reader(f)
                headers = next(reader)  # 读取表头
                print(f"处理文件 {csv_file}: 列名: {headers}")

                # 查找课程号和课程名、上课专业所在的列索引
                # 尝试找到包含这些关键字的列
                course_code_idx = -1
                course_name_idx = -1
                major_idx = -1

                for i, header in enumerate(headers):
                    header_lower = header.lower()
                    if '课程号' in header_lower or 'course' in header_lower:
                        course_code_idx = i
                    elif '课程名' in header_lower or '课程名称' in header_lower:
                        course_name_idx = i
                    elif '上课专业' in header_lower or '专业' in header_lower:
                        major_idx = i

                print(
                    f"处理文件 {csv_file}: 课程号列索引: {course_code_idx}, 课程名列索引: {course_name_idx}, 专业列索引: {major_idx}")

                # 如果找不到必要的列，继续尝试下一个编码
                if course_code_idx == -1 or course_name_idx == -1:
                    print(f"处理文件 {csv_file}: 使用编码 {enc} 未能找到必要的列")
                    continue

                # 处理每一行数据
                row_count = 0
                for row in reader:
                    row_count += 1
                    if len(row) <= max(course_code_idx, course_name_idx, major_idx):
                        continue  # 跳过列数不足的行

                    # 提取课程信息
                    course_code = row[course_code_idx].strip()
                    course_name = row[course_name_idx].strip()

                    if course_code and course_name:
                        # 如果课程号已存在，检查课程名是否一致
                        if course_code in courses:
                            if courses[course_code] != course_name:
                                print(
                                    f"警告: 课程号 {course_code} 在文件 {csv_file} 中有不同的课程名: '{courses[course_code]}' vs '{course_name}'")
                        else:
                            courses[course_code] = course_name

                    # 提取专业信息
                    if major_idx != -1 and major_idx < len(row):
                        major_field = row[major_idx].strip()
                        if major_field:
                            # 分割专业（用逗号分割）
                            majors = [m.strip() for m in major_field.split(',')]

                            for major in majors:
                                if major and course_code:
                                    # 清理专业名称（移除括号内容）
                                    clean_major = major
                                    # 处理中文括号
                                    if '（' in clean_major:
                                        clean_major = clean_major.split('（')[0]
                                    # 处理英文括号
                                    elif '(' in clean_major:
                                        clean_major = clean_major.split('(')[0]

                                    clean_major = clean_major.strip()
                                    if clean_major:
                                        major_course_pairs.append((clean_major, course_code))
                                        all_majors.add(clean_major)

                print(f"处理文件 {csv_file}: 使用编码 {enc} 成功处理文件，共处理 {row_count} 行数据")
                break  # 成功处理，跳出循环

        except UnicodeDecodeError:
            print(f"处理文件 {csv_file}: 编码 {enc} 失败，尝试下一个编码...")
            continue
        except Exception as e:
            print(f"处理文件 {csv_file}: 使用编码 {enc} 时发生错误: {e}")
            continue

    return courses, major_course_pairs, all_majors


def process_multiple_files(csv_files, predefined_majors_file=None):
    """处理多个CSV文件，合并数据"""

    all_courses = {}  # 合并后的课程字典
    all_major_course_pairs = []  # 合并后的专业-课程对
    all_majors = set()  # 合并后的专业集合

    # 如果提供了预定义的专业列表文件，读取并添加到专业集合中
    if predefined_majors_file and os.path.exists(predefined_majors_file):
        try:
            with open(predefined_majors_file, 'r', encoding='utf-8') as f:
                for line in f:
                    major = line.strip()
                    if major and not major.startswith('--') and not major.startswith('#'):
                        all_majors.add(major)
            print(f"从预定义文件 {predefined_majors_file} 加载了 {len(all_majors)} 个专业")
        except Exception as e:
            print(f"读取预定义专业文件失败: {e}")

    # 处理每个CSV文件
    for csv_file in csv_files:
        if not os.path.exists(csv_file):
            print(f"警告: 文件 {csv_file} 不存在，跳过")
            continue

        print(f"\n{'=' * 50}")
        print(f"开始处理文件: {csv_file}")
        print(f"{'=' * 50}")

        courses, pairs, majors = process_courses(csv_file, all_majors)

        # 合并课程数据
        for code, name in courses.items():
            if code in all_courses:
                if all_courses[code] != name:
                    print(f"警告: 课程号 {code} 在不同文件中有不同的课程名: '{all_courses[code]}' vs '{name}'")
            else:
                all_courses[code] = name

        # 合并专业-课程对
        all_major_course_pairs.extend(pairs)

        # 更新专业集合
        all_majors.update(majors)

        print(f"文件 {csv_file} 处理完成: 找到 {len(courses)} 个课程, {len(pairs)} 个专业-课程对")

    print(f"\n{'=' * 50}")
    print("所有文件处理完成!")
    print(f"总计: {len(all_courses)} 个课程, {len(all_major_course_pairs)} 个专业-课程对, {len(all_majors)} 个独特专业")
    print(f"{'=' * 50}")

    return all_courses, all_major_course_pairs, all_majors


def generate_sql(courses, major_course_pairs, all_majors):
    """生成SQL插入语句"""

    # 生成专业表插入语句
    # 为每个专业分配一个ID（从1开始）
    major_id_map = {}  # 专业名 -> 专业ID
    major_sql = []

    for i, major in enumerate(sorted(all_majors), 1):
        major_id_map[major] = i
        major_escaped = major.replace("'", "''")
        sql = f"INSERT INTO major(major_no, major_name) VALUES ({i}, '{major_escaped}');"
        major_sql.append(sql)

    # 生成课程表插入语句
    course_sql = []
    for code, name in courses.items():
        # 转义单引号
        name_escaped = name.replace("'", "''")
        sql = f"INSERT INTO course(course_no, course_name) VALUES ('{code}', '{name_escaped}');"
        course_sql.append(sql)

    # 生成专业-课程对插入语句
    # 先对专业-课程对去重
    unique_pairs = list(set(major_course_pairs))

    pair_sql = []
    for major, code in unique_pairs:
        if major in major_id_map:
            major_id = major_id_map[major]
            sql = f"INSERT INTO major_course(major_no, course_no) VALUES ({major_id}, '{code}');"
            pair_sql.append(sql)
        else:
            print(f"警告: 专业 '{major}' 不在专业列表中，跳过插入专业-课程对")

    return major_sql, course_sql, pair_sql, unique_pairs, major_id_map


def save_results(major_sql, course_sql, pair_sql, unique_pairs, major_id_map, courses, pairs, majors):
    """保存生成的结果到文件"""

    # 保存到文件
    with open('majors_insert.sql', 'w', encoding='utf-8') as f:
        f.write("-- 专业表插入语句\n")
        f.write("-- 专业ID为自增，这里手动指定了ID以便后续使用\n")
        f.write("-- 生成时间: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + "\n\n")
        for sql in major_sql:
            f.write(sql + '\n')

    with open('courses_insert.sql', 'w', encoding='utf-8') as f:
        f.write("-- 课程表插入语句\n")
        f.write("-- 注意: 请确保课程表中没有重复的课程号\n")
        f.write("-- 生成时间: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + "\n\n")
        for sql in course_sql:
            f.write(sql + '\n')

    with open('major_course_insert.sql', 'w', encoding='utf-8') as f:
        f.write("-- 专业-课程对插入语句\n")
        f.write("-- 使用格式: (专业ID, 课程号)\n")
        f.write("-- 生成时间: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + "\n\n")
        for sql in pair_sql:
            f.write(sql + '\n')

    # 保存专业ID映射关系
    with open('major_id_mapping.csv', 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['major_id', 'major_name'])
        for major, major_id in sorted(major_id_map.items(), key=lambda x: x[1]):
            writer.writerow([major_id, major])

    # 保存原始专业-课程对应关系（包含重复）
    with open('major_course_raw_pairs.csv', 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['专业名称', '课程号'])
        for major, code in pairs:
            writer.writerow([major, code])

    # 保存所有课程列表
    with open('all_courses.csv', 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['课程号', '课程名称'])
        for code, name in sorted(courses.items()):
            writer.writerow([code, name])

    # 保存所有专业列表
    with open('all_majors.txt', 'w', encoding='utf-8') as f:
        f.write("-- 所有专业名称列表\n")
        f.write("-- 建议先检查这些专业是否都已存在于专业表中\n")
        f.write("-- 生成时间: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + "\n\n")
        for major in sorted(majors):
            f.write(major + '\n')


# 使用示例
if __name__ == "__main__":
    from datetime import datetime

    # 定义要处理的CSV文件列表
    csv_files = [
        "24-25-1全校课程表.csv",
        "24-25-2全校课程表.csv"
    ]

    # 预定义的专业列表文件（可选）
    predefined_majors_file = "predefined_majors.txt"

    print("开始处理多个CSV文件...")
    all_courses, all_pairs, all_majors = process_multiple_files(csv_files, predefined_majors_file)

    print(f"\n合并后的数据统计:")
    print(f"课程总数: {len(all_courses)}")
    print(f"专业-课程对总数: {len(all_pairs)}")
    print(f"独特专业总数: {len(all_majors)}")

    # 生成SQL
    major_sql, course_sql, pair_sql, unique_pairs, major_id_map = generate_sql(all_courses, all_pairs, all_majors)

    # 保存结果
    save_results(major_sql, course_sql, pair_sql, unique_pairs, major_id_map, all_courses, all_pairs, all_majors)

    print("\n生成的文件:")
    print("1. majors_insert.sql - 专业表插入语句")
    print("2. courses_insert.sql - 课程表插入语句")
    print("3. major_course_insert.sql - 专业-课程对插入语句 (格式: 专业ID, 课程号)")
    print("4. major_id_mapping.csv - 专业ID映射关系")
    print("5. major_course_raw_pairs.csv - 原始专业-课程对应关系（CSV格式，包含重复）")
    print("6. all_courses.csv - 所有课程列表")
    print("7. all_majors.txt - 所有专业名称列表")

    # 显示一些示例
    if major_id_map:
        print("\n=== 前10个专业ID映射 ===")
        for i, (major, major_id) in enumerate(list(major_id_map.items())[:10]):
            print(f"{major_id}: {major}")

    if all_courses:
        print("\n=== 前5个课程 ===")
        for i, (code, name) in enumerate(list(all_courses.items())[:5]):
            print(f"{code}: {name}")

    if all_pairs:
        print("\n=== 前10个专业-课程对应关系 ===")
        for i, (major, code) in enumerate(all_pairs[:10]):
            major_id = major_id_map.get(major, 'N/A')
            print(f"专业ID {major_id} ({major}) -> {code}")

    # 显示一些统计信息
    print("\n=== 统计信息 ===")
    print(f"专业总数: {len(major_id_map)}")
    print(f"课程总数: {len(all_courses)}")
    print(f"去重后的专业-课程对数量: {len(unique_pairs)}")

    # 检查是否有课程没有专业
    courses_without_majors = set(all_courses.keys()) - set([code for _, code in unique_pairs])
    if courses_without_majors:
        print(f"有 {len(courses_without_majors)} 个课程没有关联专业")
        print("示例:", list(courses_without_majors)[:5])

    # 检查是否有专业没有课程
    majors_without_courses = set(major_id_map.keys()) - set([major for major, _ in unique_pairs])
    if majors_without_courses:
        print(f"有 {len(majors_without_courses)} 个专业没有关联课程")
        print("示例:", list(majors_without_courses)[:5])