#!/usr/bin/env python3

import numpy as np
import matplotlib.pyplot as plt
import sys
import collections
import glob
import subprocess
import warnings

apps = ['barometer', 'bible', 'dpm', 'drumpads', 'equibase', 'localtv', 'loctracker', 'mitula', 'moonphases',\
			'parking', 'parrot', 'post', 'quicknews', 'speedlogic', 'vidanta']

def read_from_files(pattern):
	error_all = []
	error_hot = []
	precision = []
	recall = []
	for file in glob.glob(pattern):
		with open(file, 'r') as f:
			for line in f.readlines():
				k, v = line.split(':')
				if k == 'Error All':
					error_all.append(float(v))
				elif k == 'Error Hot':
					error_hot.append(float(v))
				elif k == 'Recall':
					recall.append(float(v))
				elif k == 'Precision':
					precision.append(float(v))
				else:
					raise Exception('Error')
	return error_all, error_hot, recall, precision


def get_ci(values):
	if len(values) < 30:
		return np.nan
	elif len(values) == 30:
		output = subprocess.check_output(['java', '-cp', 'app/build/libs/app-1.0-SNAPSHOT-all.jar', 'presto.Statistics', \
			*[str(x) for x in values]])
		return float(output.decode('utf-8'))
	else:
		raise Exception('More than 30 values')


def load_data(app):
	data = {}

	error_all, error_hot, recall, precision = read_from_files('results/%s-cca-u1000-e9-relaxed-run*.txt' % app)
	data['re_all_1k_cc'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_ci_1k_cc'] = get_ci(error_all)
	data['re_hot_1k_cc'] = np.mean(error_hot) if len(error_hot) > 0 else np.nan
	data['re_hot_ci_1k_cc'] = get_ci(error_hot)
	data['recall_1k_cc'] = np.mean(recall) if len(recall) > 0 else np.nan
	data['recall_ci_1k_cc'] = get_ci(recall)
	data['precision_1k_cc'] = np.mean(precision) if len(precision) > 0 else np.nan
	data['precision_ci_1k_cc'] = get_ci(precision)

	error_all, error_hot, recall, precision = read_from_files('results/%s-cca-u10000-e9-relaxed-run*.txt' % app)
	data['re_all_10k_cc'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_ci_10k_cc'] = get_ci(error_all)
	data['re_hot_10k_cc'] = np.mean(error_hot) if len(error_hot) > 0 else np.nan
	data['re_hot_ci_10k_cc'] = get_ci(error_hot)
	data['recall_10k_cc'] = np.mean(recall) if len(recall) > 0 else np.nan
	data['recall_ci_10k_cc'] = get_ci(recall)
	data['precision_10k_cc'] = np.mean(precision) if len(precision) > 0 else np.nan
	data['precision_ci_10k_cc'] = get_ci(precision)

	error_all, error_hot, recall, precision = read_from_files('results/%s-eeta-u1000-e9-relaxed-run*.txt' % app)
	data['re_all_1k_eet'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_ci_1k_eet'] = get_ci(error_all)
	data['re_hot_1k_eet'] = np.mean(error_hot) if len(error_hot) > 0 else np.nan
	data['re_hot_ci_1k_eet'] = get_ci(error_hot)
	data['recall_1k_eet'] = np.mean(recall) if len(recall) > 0 else np.nan
	data['recall_ci_1k_eet'] = get_ci(recall)
	data['precision_1k_eet'] = np.mean(precision) if len(precision) > 0 else np.nan
	data['precision_ci_1k_eet'] = get_ci(precision)

	error_all, error_hot, recall, precision = read_from_files('results/%s-eeta-u10000-e9-relaxed-run*.txt' % app)
	data['re_all_10k_eet'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_ci_10k_eet'] = get_ci(error_all)
	data['re_hot_10k_eet'] = np.mean(error_hot) if len(error_hot) > 0 else np.nan
	data['re_hot_ci_10k_eet'] = get_ci(error_hot)
	data['recall_10k_eet'] = np.mean(recall) if len(recall) > 0 else np.nan
	data['recall_ci_10k_eet'] = get_ci(recall)
	data['precision_10k_eet'] = np.mean(precision) if len(precision) > 0 else np.nan
	data['precision_ci_10k_eet'] = get_ci(precision)

	error_all, error_hot, recall, precision = read_from_files('results/%s-cca-u1000-e9-strict-run*.txt' % app)
	data['recall_strict_1k_cc'] = np.mean(recall) if len(recall) > 0 else np.nan
	data['recall_strict_ci_1k_cc'] = get_ci(recall)
	data['precision_strict_1k_cc'] = np.mean(precision) if len(precision) > 0 else np.nan
	data['precision_strict_ci_1k_cc'] = get_ci(precision)

	error_all, error_hot, recall, precision = read_from_files('results/%s-eeta-u1000-e9-strict-run*.txt' % app)
	data['recall_strict_1k_eet'] = np.mean(recall) if len(recall) > 0 else np.nan
	data['recall_strict_ci_1k_eet'] = get_ci(recall)
	data['precision_strict_1k_eet'] = np.mean(precision) if len(precision) > 0 else np.nan
	data['precision_strict_ci_1k_eet'] = get_ci(precision)

	error_all, error_hot, recall, precision = read_from_files('results/%s-cca-u1000-e3-relaxed-run*.txt' % app)
	data['re_all_e3_1k_cc'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_e3_ci_1k_cc'] = get_ci(error_all)

	error_all, error_hot, recall, precision = read_from_files('results/%s-cca-u1000-e49-relaxed-run*.txt' % app)
	data['re_all_e49_1k_cc'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_e49_ci_1k_cc'] = get_ci(error_all)

	error_all, error_hot, recall, precision = read_from_files('results/%s-eeta-u1000-e3-relaxed-run*.txt' % app)
	data['re_all_e3_1k_eet'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_e3_ci_1k_eet'] = get_ci(error_all)

	error_all, error_hot, recall, precision = read_from_files('results/%s-eeta-u1000-e49-relaxed-run*.txt' % app)
	data['re_all_e49_1k_eet'] = np.mean(error_all) if len(error_all) > 0 else np.nan
	data['re_all_e49_ci_1k_eet'] = get_ci(error_all)

	return data



def main():
	warnings.simplefilter("ignore", UserWarning)

	data = {}
	for app in apps:
		data[app] = load_data(app)

	plot_fig2(data)

	plot_fig3(data)

	plot_fig4(data)

	plot_fig5(data)

	plot_fig6(data)

	plt.show()


def get_one_data_all_apps(data, key):
	return np.array([data[app][key] for app in apps])


def plot_fig2(data):
	re_all_1k_cc = get_one_data_all_apps(data, 're_all_1k_cc')
	re_all_ci_1k_cc = get_one_data_all_apps(data, 're_all_ci_1k_cc')
	re_all_10k_cc = get_one_data_all_apps(data, 're_all_10k_cc')
	re_all_ci_10k_cc = get_one_data_all_apps(data, 're_all_ci_10k_cc')

	re_all_1k_eet = get_one_data_all_apps(data, 're_all_1k_eet')
	re_all_ci_1k_eet = get_one_data_all_apps(data, 're_all_ci_1k_eet')
	re_all_10k_eet = get_one_data_all_apps(data, 're_all_10k_eet')
	re_all_ci_10k_eet = get_one_data_all_apps(data, 're_all_ci_10k_eet')

	fig, ax = plt.subplots(1, 2)
	x = np.arange(len(apps))  # the label locations
	width = 0.35  # the width of the bars

	ax[0].bar(x - width/2, re_all_1k_cc, width, label='1000 users', 
		yerr=re_all_ci_1k_cc, capsize=1.5)
	ax[0].bar(x + width/2, re_all_10k_cc, width, label='10000 users', 
		yerr=re_all_ci_10k_cc, capsize=1.5, hatch='///')
	ax[0].set_ylabel('Error All')
	ax[0].set_title('Call chains')
	ax[0].set_xticks(x)
	ax[0].set_xticklabels(apps, rotation=90)
	ax[0].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=2)

	ax[1].bar(x - width/2, re_all_1k_eet, width, label='1000 users', 
		yerr=re_all_ci_1k_eet, capsize=1.5)
	ax[1].bar(x + width/2, re_all_10k_eet, width, label='10000 users', 
		yerr=re_all_ci_10k_eet, capsize=1.5, hatch='///')
	ax[1].set_ylabel('Error All')
	ax[1].set_title('Enter/exit traces')
	ax[1].set_xticks(x)
	ax[1].set_xticklabels(apps, rotation=90)
	ax[1].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=2)

	fig.suptitle('fig 2')
	fig.tight_layout()

	plt.savefig('fig2.pdf')


def plot_fig3(data):
	recall_1k_cc = get_one_data_all_apps(data, 'recall_1k_cc')
	recall_ci_1k_cc = get_one_data_all_apps(data, 'recall_ci_1k_cc')
	recall_10k_cc = get_one_data_all_apps(data, 'recall_10k_cc')
	recall_ci_10k_cc = get_one_data_all_apps(data, 'recall_ci_10k_cc')

	precision_1k_cc = get_one_data_all_apps(data, 'precision_1k_cc')
	precision_ci_1k_cc = get_one_data_all_apps(data, 'precision_ci_1k_cc')
	precision_10k_cc = get_one_data_all_apps(data, 'precision_10k_cc')
	precision_ci_10k_cc = get_one_data_all_apps(data, 'precision_ci_10k_cc')

	recall_1k_eet = get_one_data_all_apps(data, 'recall_1k_eet')
	recall_ci_1k_eet = get_one_data_all_apps(data, 'recall_ci_1k_eet')
	recall_10k_eet = get_one_data_all_apps(data, 'recall_10k_eet')
	recall_ci_10k_eet = get_one_data_all_apps(data, 'recall_ci_10k_eet')

	precision_1k_eet = get_one_data_all_apps(data, 'precision_1k_eet')
	precision_ci_1k_eet = get_one_data_all_apps(data, 'precision_ci_1k_eet')
	precision_10k_eet = get_one_data_all_apps(data, 'precision_10k_eet')
	precision_ci_10k_eet = get_one_data_all_apps(data, 'precision_ci_10k_eet')

	fig, ax = plt.subplots(2, 2)
	x = np.arange(len(apps))  # the label locations
	width = 0.35  # the width of the bars

	ax[0][0].bar(x - width/2, recall_1k_cc, width, label='1000 users', 
		yerr=recall_ci_1k_cc, capsize=1.5)
	ax[0][0].bar(x + width/2, recall_10k_cc, width, label='10000 users', 
		yerr=recall_ci_10k_cc, capsize=1.5, hatch='///')
	ax[0][0].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[0][0].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[0][0].set_yticks([0, 0.5, 1.0])
	ax[0][0].set_ylabel('Recall')
	ax[0][0].set_xticks(x)
	ax[0][0].tick_params(labelbottom=False) 
	ax[0][0].legend(loc=9, bbox_to_anchor=(0.5, -0.1), ncol=2)
	ax[0][0].set_title('Call chains')

	ax[1][0].bar(x - width/2, precision_1k_cc, width, label='1000 users', 
		yerr=precision_ci_1k_cc, capsize=1.5)
	ax[1][0].bar(x + width/2, precision_10k_cc, width, label='10000 users', 
		yerr=precision_ci_10k_cc, capsize=1.5, hatch='///')
	ax[1][0].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[1][0].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[1][0].set_yticks([0, 0.5, 1.0])
	ax[1][0].set_ylabel('Precision')
	ax[1][0].set_xticks(x)
	ax[1][0].set_xticklabels(apps, rotation=90)

	ax[0][1].bar(x - width/2, recall_1k_eet, width, label='1000 users', 
		yerr=recall_ci_1k_eet, capsize=1.5)
	ax[0][1].bar(x + width/2, recall_10k_eet, width, label='10000 users', 
		yerr=recall_ci_10k_eet, capsize=1.5, hatch='///')
	ax[0][1].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[0][1].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[0][1].set_yticks([0, 0.5, 1.0])
	ax[0][1].set_ylabel('Recall')
	ax[0][1].set_xticks(x)
	ax[0][1].tick_params(labelbottom=False) 
	ax[0][1].legend(loc=9, bbox_to_anchor=(0.5, -0.1), ncol=2)
	ax[0][1].set_title('Enter/exit traces')

	ax[1][1].bar(x - width/2, precision_1k_eet, width, label='1000 users', 
		yerr=precision_ci_1k_eet, capsize=1.5)
	ax[1][1].bar(x + width/2, precision_10k_eet, width, label='10000 users', 
		yerr=precision_ci_10k_eet, capsize=1.5, hatch='///')
	ax[1][1].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[1][1].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[1][1].set_yticks([0, 0.5, 1.0])
	ax[1][1].set_ylabel('Precision')
	ax[1][1].set_xticks(x)
	ax[1][1].set_xticklabels(apps, rotation=90)

	fig.suptitle('fig 3')
	fig.tight_layout()

	plt.savefig('fig3.pdf')


def plot_fig4(data):
	recall_1k_cc = get_one_data_all_apps(data, 'recall_1k_cc')
	recall_ci_1k_cc = get_one_data_all_apps(data, 'recall_ci_1k_cc')
	recall_strict_1k_cc = get_one_data_all_apps(data, 'recall_strict_1k_cc')
	recall_strict_ci_1k_cc = get_one_data_all_apps(data, 'recall_strict_ci_1k_cc')

	precision_1k_cc = get_one_data_all_apps(data, 'precision_1k_cc')
	precision_ci_1k_cc = get_one_data_all_apps(data, 'precision_ci_1k_cc')
	precision_strict_1k_cc = get_one_data_all_apps(data, 'precision_strict_1k_cc')
	precision_strict_ci_1k_cc = get_one_data_all_apps(data, 'precision_strict_ci_1k_cc')

	recall_1k_eet = get_one_data_all_apps(data, 'recall_1k_eet')
	recall_ci_1k_eet = get_one_data_all_apps(data, 'recall_ci_1k_eet')
	recall_strict_1k_eet = get_one_data_all_apps(data, 'recall_strict_1k_eet')
	recall_strict_ci_1k_eet = get_one_data_all_apps(data, 'recall_strict_ci_1k_eet')

	precision_1k_eet = get_one_data_all_apps(data, 'precision_1k_eet')
	precision_ci_1k_eet = get_one_data_all_apps(data, 'precision_ci_1k_eet')
	precision_strict_1k_eet = get_one_data_all_apps(data, 'precision_strict_1k_eet')
	precision_strict_ci_1k_eet = get_one_data_all_apps(data, 'precision_strict_ci_1k_eet')

	fig, ax = plt.subplots(2, 2)
	x = np.arange(len(apps))  # the label locations
	width = 0.35  # the width of the bars

	ax[0][0].bar(x - width/2, recall_strict_1k_cc, width, label='strict', 
		yerr=recall_strict_ci_1k_cc, capsize=1.5)
	ax[0][0].bar(x + width/2, recall_1k_cc, width, label='relaxed', 
		yerr=recall_ci_1k_cc, capsize=1.5, hatch='///')
	ax[0][0].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[0][0].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[0][0].set_yticks([0, 0.5, 1.0])
	ax[0][0].set_ylabel('Recall')
	ax[0][0].set_xticks(x)
	ax[0][0].tick_params(labelbottom=False) 
	ax[0][0].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=2)
	ax[0][0].set_title('Call chains')

	ax[1][0].bar(x - width/2, precision_strict_1k_cc, width, label='strict', 
		yerr=precision_strict_ci_1k_cc, capsize=1.5)
	ax[1][0].bar(x + width/2, precision_1k_cc, width, label='relaxed', 
		yerr=precision_ci_1k_cc, capsize=1.5, hatch='///')
	ax[1][0].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[1][0].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[1][0].set_yticks([0, 0.5, 1.0])
	ax[1][0].set_ylabel('Precision')
	ax[1][0].set_xticks(x)
	ax[1][0].set_xticklabels(apps, rotation=90)

	ax[0][1].bar(x - width/2, recall_strict_1k_eet, width, label='strict', 
		yerr=recall_strict_ci_1k_eet, capsize=1.5)
	ax[0][1].bar(x + width/2, recall_1k_eet, width, label='relaxed', 
		yerr=recall_ci_1k_eet, capsize=1.5, hatch='///')
	ax[0][1].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[0][1].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[0][1].set_yticks([0, 0.5, 1.0])
	ax[0][1].set_ylabel('Recall')
	ax[0][1].set_xticks(x)
	ax[0][1].tick_params(labelbottom=False) 
	ax[0][1].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=2)
	ax[0][1].set_title('Enter/exit traces')

	ax[1][1].bar(x - width/2, precision_strict_1k_eet, width, label='strict', 
		yerr=precision_strict_ci_1k_eet, capsize=1.5)
	ax[1][1].bar(x + width/2, precision_1k_eet, width, label='relaxed', 
		yerr=precision_ci_1k_eet, capsize=1.5, hatch='///')
	ax[1][1].axhline(1.0, c='gray', lw=1, zorder=1)
	ax[1][1].axhline(0.5, c='gray', lw=1, zorder=1)
	ax[1][1].set_yticks([0, 0.5, 1.0])
	ax[1][1].set_ylabel('Precision')
	ax[1][1].set_xticks(x)
	ax[1][1].set_xticklabels(apps, rotation=90)

	fig.suptitle('fig 4')
	fig.tight_layout()

	plt.savefig('fig4.pdf')


def plot_fig5(data):
	re_hot_1k_cc = get_one_data_all_apps(data, 're_hot_1k_cc')
	re_hot_ci_1k_cc = get_one_data_all_apps(data, 're_hot_ci_1k_cc')
	re_hot_10k_cc = get_one_data_all_apps(data, 're_hot_10k_cc')
	re_hot_ci_10k_cc = get_one_data_all_apps(data, 're_hot_ci_10k_cc')

	re_hot_1k_eet = get_one_data_all_apps(data, 're_hot_1k_eet')
	re_hot_ci_1k_eet = get_one_data_all_apps(data, 're_hot_ci_1k_eet')
	re_hot_10k_eet = get_one_data_all_apps(data, 're_hot_10k_eet')
	re_hot_ci_10k_eet = get_one_data_all_apps(data, 're_hot_ci_10k_eet')

	fig, ax = plt.subplots(1, 2)
	x = np.arange(len(apps))  # the label locations
	width = 0.35  # the width of the bars

	ax[0].bar(x - width/2, re_hot_1k_cc, width, label='1000 users', 
		yerr=re_hot_ci_1k_cc, capsize=1.5)
	ax[0].bar(x + width/2, re_hot_10k_cc, width, label='10000 users', 
		yerr=re_hot_ci_10k_cc, capsize=1.5, hatch='///')
	ax[0].set_ylabel('Error Hot')
	ax[0].set_title('Call chains')
	ax[0].set_xticks(x)
	ax[0].set_xticklabels(apps, rotation=90)
	ax[0].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=2)

	ax[1].bar(x - width/2, re_hot_1k_eet, width, label='1000 users', 
		yerr=re_hot_ci_1k_eet, capsize=1.5)
	ax[1].bar(x + width/2, re_hot_10k_eet, width, label='10000 users', 
		yerr=re_hot_ci_10k_eet, capsize=1.5, hatch='///')
	ax[1].set_ylabel('Error Hot')
	ax[1].set_title('Enter/exit traces')
	ax[1].set_xticks(x)
	ax[1].set_xticklabels(apps, rotation=90)
	ax[1].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=2)

	fig.suptitle('fig 5')
	fig.tight_layout()

	plt.savefig('fig5.pdf')


def plot_fig6(data):
	re_all_1k_cc = get_one_data_all_apps(data, 're_all_1k_cc')
	re_all_ci_1k_cc = get_one_data_all_apps(data, 're_all_ci_1k_cc')
	re_all_e3_1k_cc = get_one_data_all_apps(data, 're_all_e3_1k_cc')
	re_all_e3_ci_1k_cc = get_one_data_all_apps(data, 're_all_e3_ci_1k_cc')
	re_all_e49_1k_cc = get_one_data_all_apps(data, 're_all_e49_1k_cc')
	re_all_e49_ci_1k_cc = get_one_data_all_apps(data, 're_all_e49_ci_1k_cc')

	re_all_1k_eet = get_one_data_all_apps(data, 're_all_1k_eet')
	re_all_ci_1k_eet = get_one_data_all_apps(data, 're_all_ci_1k_eet')
	re_all_e3_1k_eet = get_one_data_all_apps(data, 're_all_e3_1k_eet')
	re_all_e3_ci_1k_eet = get_one_data_all_apps(data, 're_all_e3_ci_1k_eet')
	re_all_e49_1k_eet = get_one_data_all_apps(data, 're_all_e49_1k_eet')
	re_all_e49_ci_1k_eet = get_one_data_all_apps(data, 're_all_e49_ci_1k_eet')

	fig, ax = plt.subplots(1, 2)
	x = np.arange(len(apps))  # the label locations
	width = 0.35  # the width of the bars

	ax[0].bar(x - width, re_all_e3_1k_cc, width, label='ln(3)', 
		yerr=re_all_e3_ci_1k_cc, capsize=1.5)
	ax[0].bar(x, re_all_1k_cc, width, label='ln(9)', 
		yerr=re_all_ci_1k_cc, capsize=1.5, hatch='///')
	ax[0].bar(x + width, re_all_e49_1k_cc, width, label='ln(49)', 
		yerr=re_all_e49_ci_1k_cc, capsize=1.5, hatch='...')
	ax[0].set_ylabel('Error All')
	ax[0].set_title('Call chains')
	ax[0].set_xticks(x)
	ax[0].set_xticklabels(apps, rotation=90)
	ax[0].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=3)

	ax[1].bar(x - width, re_all_e3_1k_eet, width, label='ln(3)', 
		yerr=re_all_e3_ci_1k_eet, capsize=1.5)
	ax[1].bar(x, re_all_1k_eet, width, label='ln(9)', 
		yerr=re_all_ci_1k_eet, capsize=1.5, hatch='///')
	ax[1].bar(x + width, re_all_e49_1k_eet, width, label='ln(49)', 
		yerr=re_all_e49_ci_1k_eet, capsize=1.5, hatch='...')
	ax[1].set_ylabel('Error All')
	ax[1].set_title('Enter/exit traces')
	ax[1].set_xticks(x)
	ax[1].set_xticklabels(apps, rotation=90)
	ax[1].legend(loc=9, bbox_to_anchor=(0.5, 1.4), ncol=3)

	fig.suptitle('fig 6')
	fig.tight_layout()

	plt.savefig('fig6.pdf')


if __name__ == '__main__':
	main()
